package com.zhanhe.workforce.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 本地文件存储：头像等小文件直接落到服务器磁盘，由 Spring MVC 以静态资源方式对外提供。
 *
 * <p>原来是 FastDFS：多一台 tracker/storage 与 nginx 才能跑，本地开发和演示环境根本起不来，
 * 表现为「点头像没反应」。这里换成本地目录 + 资源映射，功能等价、零外部依赖；
 * 真要上集群，把这个类换成对象存储实现即可，调用方不用改。</p>
 */
@Component
public class LocalFileStorage
{
    private static final Logger log = LoggerFactory.getLogger(LocalFileStorage.class);

    /** 只允许图片，避免上传可执行内容 */
    private static final List<String> ALLOWED = Arrays.asList("png", "jpg", "jpeg", "gif", "webp", "bmp");

    private static final long MAX_SIZE = 5L * 1024 * 1024;

    @Value("${vhr.upload.dir:./uploads}")
    private String uploadDir;

    /** 保存并返回可直接访问的相对路径，例如 /uploads/20261109/xxx.png */
    public String save(MultipartFile file) throws IOException
    {
        if (file == null || file.isEmpty())
        {
            throw new IOException("上传文件为空");
        }
        if (file.getSize() > MAX_SIZE)
        {
            throw new IOException("图片不能超过 5MB");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT)
                : "";
        if (!ALLOWED.contains(ext))
        {
            throw new IOException("只支持图片格式：" + String.join("/", ALLOWED));
        }

        String day = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Path dir = rootPath().resolve(day);
        Files.createDirectories(dir);

        String name = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path target = dir.resolve(name);
        file.transferTo(target.toFile());
        log.info("avatar saved: {}", target);
        return "/uploads/" + day + "/" + name;
    }

    /** 上传根目录的绝对路径，资源映射与保存共用，避免两处口径不一致 */
    public Path rootPath() throws IOException
    {
        File dir = new File(uploadDir);
        if (!dir.isAbsolute())
        {
            // 相对路径按工作目录解析，启动脚本与 IDEA 运行都能落在项目目录下
            dir = Paths.get(System.getProperty("user.dir"), uploadDir).toFile();
        }
        Files.createDirectories(dir.toPath());
        return dir.toPath().toAbsolutePath().normalize();
    }

    public String getUploadDir()
    {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir)
    {
        this.uploadDir = uploadDir;
    }
}
