# Projeto Exemplo JSON para XLSX

Aplicação Spring Boot que consulta uma API de origem e exporta os dados para XLSX.
O projeto Maven está no diretório `demo`.

## Configuração segura da API de origem

O endpoint público é:

```text
GET /export/xlsx/{id}
```

O cliente não pode mais escolher o destino HTTP. O parâmetro legado `baseUrl` é
rejeitado com HTTP 400 e nunca é usado em uma chamada de rede.

Configure o destino exclusivamente no ambiente do servidor:

```bash
export EXPORT_API_BASE_URL="https://api.example.com:443"
export EXPORT_API_ALLOWED_SCHEMES="https"
export EXPORT_API_ALLOWED_AUTHORITIES="api.example.com:443"
```

No PowerShell:

```powershell
$env:EXPORT_API_BASE_URL = "https://api.example.com:443"
$env:EXPORT_API_ALLOWED_SCHEMES = "https"
$env:EXPORT_API_ALLOWED_AUTHORITIES = "api.example.com:443"
```

`EXPORT_API_ALLOWED_AUTHORITIES` é uma allowlist explícita de pares `host:porta`,
separados por vírgula. Não há wildcard. Cada destino precisa corresponder
exatamente a uma entrada.

HTTPS é o único esquema permitido por padrão. Para um ambiente de desenvolvimento
que realmente precise de HTTP, habilite-o de forma explícita:

```text
EXPORT_API_ALLOWED_SCHEMES=http,https
```

Mesmo quando habilitado, HTTP continua sujeito à allowlist e ao bloqueio de
endereços não públicos. `localhost`, loopback, link-local, redes privadas,
multicast e faixas reservadas não são aceitos. Portanto, um mock local não deve
ser habilitado por relaxamento da proteção em uma implantação pública; use um
host de desenvolvimento isolado e explicitamente autorizado.

## Proteções SSRF

O fluxo do destino é:

```text
application.properties / variáveis de ambiente
  -> ExportApiProperties
  -> ApiDestinationValidator
  -> ApiHttpClient
  -> RestTemplate com redirects desativados
```

As camadas de controller, montagem das planilhas e serviços de domínio não
recebem mais `baseUrl`.

Antes de cada página consultada, `ApiDestinationValidator`:

- aceita apenas os esquemas configurados (`https` por padrão; opcionalmente `http`);
- exige correspondência exata de host e porta com a allowlist;
- rejeita credenciais, query string, fragmento, whitespace, barras invertidas,
  portas inválidas, caminhos ambíguos e URLs malformadas;
- rejeita protocolos como `file`, `jar`, `ftp` e `gopher`;
- resolve o DNS e valida todos os endereços retornados;
- bloqueia IPv4 e IPv6 locais, privados, link-local, multicast, documentação e
  demais faixas especiais tratadas pelo validador.

O cliente HTTP usa `HttpClient.Redirect.NEVER`; respostas 3xx não são seguidas.
URLs completas, tokens, credenciais e parâmetros de consulta não são escritos nos
logs nem devolvidos ao cliente.

## Limitação de DNS rebinding

A validação é repetida imediatamente antes de cada chamada, reduzindo a janela
para DNS rebinding. Ainda existe uma janela de tempo entre a resolução feita pelo
validador e a resolução/conexão interna do cliente HTTP. Para defesa adicional em
ambientes de alto risco, use DNS interno controlado e filtragem de saída
(firewall/proxy) que permita conexão somente aos IPs da API autorizada.

## Testes

Requer Java 21:

```bash
cd demo
./mvnw test
```

No Windows:

```powershell
cd demo
.\mvnw.cmd test
```

O workflow `.github/workflows/maven.yml` executa os testes com Java 21 em pushes
e pull requests.
