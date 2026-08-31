/**
 * 分类配图上传接口集成测试。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.api.admin;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminTaxonMediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void uploadRequiresAuth() throws Exception {
        MockMultipartFile file = pngFile();
        mockMvc.perform(multipart("/api/admin/taxa/{taxonId}/media", 8).file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanUploadAndDeleteMedia() throws Exception {
        MockMultipartFile file = pngFile();
        String body = mockMvc.perform(multipart("/api/admin/taxa/{taxonId}/media", 8)
                        .file(file)
                        .param("caption", "demo")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").exists())
                .andExpect(jsonPath("$.data.caption").value("demo"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long mediaId = Long.parseLong(body.replaceAll("(?s).*\"id\":(\\d+).*", "$1"));

        mockMvc.perform(get("/api/taxa/{id}", 8).param("locale", "zh-CN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.media.length()").value(1));

        mockMvc.perform(delete("/api/admin/taxa/{taxonId}/media/{mediaId}", 8, mediaId)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void rejectNonImage() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile("file", "note.txt", "text/plain", "hello".getBytes());
        mockMvc.perform(multipart("/api/admin/taxa/{taxonId}/media", 8)
                        .file(file)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    private static MockMultipartFile pngFile() throws Exception {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, Color.GREEN.getRGB());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return new MockMultipartFile("file", "demo.png", "image/png", out.toByteArray());
    }
}
