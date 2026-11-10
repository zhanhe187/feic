package com.zhanhe.workforce.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/**
 * 头像本地存储：格式白名单、大小限制、落盘路径与访问 URL 的对应关系。
 */
class LocalFileStorageTest
{
    private LocalFileStorage storage;

    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException
    {
        tempDir = Files.createTempDirectory("vhr-upload-test");
        storage = new LocalFileStorage();
        storage.setUploadDir(tempDir.toString());
    }

    @AfterEach
    void tearDown() throws IOException
    {
        if (tempDir != null && Files.exists(tempDir))
        {
            Files.walk(tempDir)
                    .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(p -> {
                        try
                        {
                            Files.deleteIfExists(p);
                        }
                        catch (IOException ignored)
                        {
                        }
                    });
        }
    }

    @Test
    void savesImageAndReturnsUrl() throws IOException
    {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png",
                new byte[] { 1, 2, 3, 4 });
        String url = storage.save(file);
        assertNotNull(url);
        assertTrue(url.startsWith("/uploads/"), "URL 应以 /uploads/ 开头: " + url);
        assertTrue(url.endsWith(".png"), "URL 应保留扩展名: " + url);

        String relative = url.substring("/uploads/".length());
        File saved = tempDir.resolve(relative).toFile();
        assertTrue(saved.exists(), "文件应已落盘");
        assertTrue(saved.length() == 4, "落盘内容长度应与上传一致");
    }

    @Test
    void rejectsNonImage() throws IOException
    {
        MockMultipartFile file = new MockMultipartFile("file", "shell.jsp", "text/plain",
                new byte[] { 1 });
        IOException e = assertThrows(IOException.class, () -> storage.save(file));
        assertTrue(e.getMessage().contains("图片"), "应提示只支持图片: " + e.getMessage());
    }

    @Test
    void rejectsOversizedFile() throws IOException
    {
        byte[] big = new byte[6 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", big);
        IOException e = assertThrows(IOException.class, () -> storage.save(file));
        assertTrue(e.getMessage().contains("5MB"), "应提示大小限制: " + e.getMessage());
    }

    @Test
    void rejectsEmptyFile() throws IOException
    {
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);
        IOException e = assertThrows(IOException.class, () -> storage.save(file));
        assertTrue(e.getMessage().contains("为空"), e.getMessage());
    }
}
