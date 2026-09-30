package io.github.danielveloso8.walletdashboard;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "spring.datasource.url=jdbc:h2:mem:wallet-lifecycle;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class ImportLifecycleTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void stagesAndCommitsTheSyntheticWalletExport() throws Exception {
        var fixture = new ClassPathResource("fixtures/wallet-synthetic.csv");
        var file = new MockMultipartFile("file", "wallet-synthetic.csv", "text/csv", fixture.getInputStream());
        mvc.perform(multipart("/api/imports").file(file)
                        .header("Host", "127.0.0.1:8080")
                        .header("X-Requested-With", "wallet-dashboard"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accounts").isArray());
        long id = jdbc.queryForObject("SELECT id FROM import_batch WHERE status='STAGED'", Long.class);
        mvc.perform(post("/api/imports/{id}/commit", id)
                        .header("Host", "127.0.0.1:8080")
                        .header("X-Requested-With", "wallet-dashboard")
                        .contentType("application/json").content("{\"confirmRemovals\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMMITTED"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM active_txn", Long.class)).isEqualTo(40L);
        mvc.perform(get("/api/dashboard/summary").queryParam("from", "2026-09-01").queryParam("to", "2026-09-30")
                        .header("Host", "127.0.0.1:8080"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currencies.length()").value(2))
                .andExpect(jsonPath("$.coverage.complete").value(true));

        var repeat = new MockMultipartFile("file", "wallet-synthetic.csv", "text/csv", fixture.getInputStream());
        mvc.perform(multipart("/api/imports").file(repeat)
                        .header("Host", "127.0.0.1:8080")
                        .header("X-Requested-With", "wallet-dashboard"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.unchangedCount").value(40))
                .andExpect(jsonPath("$.requiresRemovalConfirmation").value(false));
        long repeatId = jdbc.queryForObject("SELECT id FROM import_batch WHERE status='STAGED'", Long.class);
        mvc.perform(post("/api/imports/{id}/commit", repeatId)
                        .header("Host", "127.0.0.1:8080")
                        .header("X-Requested-With", "wallet-dashboard")
                        .contentType("application/json").content("{\"confirmRemovals\":false}"))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM active_txn", Long.class)).isEqualTo(40L);
    }
}
