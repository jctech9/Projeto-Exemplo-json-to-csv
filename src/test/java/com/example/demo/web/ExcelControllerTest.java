package com.example.demo.web;

import com.example.demo.application.export.ExcelService;
import com.example.demo.application.export.ExportSheetBuilderService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExcelControllerTest {

    @Test
    void shouldRejectLegacyBaseUrlWithoutCallingServices() throws Exception {
        ExcelService excelService = mock(ExcelService.class);
        ExportSheetBuilderService builderService = mock(ExportSheetBuilderService.class);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new ExcelController(excelService, builderService))
                .build();

        mockMvc.perform(get("/export/xlsx/1")
                        .queryParam("baseUrl", "http://127.0.0.1:8080"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Parametro de destino nao e aceito."));

        verifyNoInteractions(excelService, builderService);
    }
}
