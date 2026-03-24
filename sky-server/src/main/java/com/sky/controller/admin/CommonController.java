package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * 通用接口
 */
@RestController
@RequestMapping("/admin/common" )
@Api(tags = "通用接口" )
@Slf4j
public class CommonController {

    @Autowired
    private AliOssUtil aliOssUtil;

    /**
     * 文件上传
     * @param file
     * @return
     */
    @PostMapping("/upload" )
    @ApiOperation("文件上传")
    public Result<String> upload(MultipartFile file) {
        log.info("文件上传：{}", file);

        //原始文件名
        String originalFilename = file.getOriginalFilename();

        //截取原始文件名后缀 dddss.jpg
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("." ) );
        log.info("文件后缀：{}", suffix);
        String objectName = UUID.randomUUID().toString() + suffix;

        try {
            String url = aliOssUtil.upload(file.getBytes(), objectName);
            log.info("文件上传成功，url：{}", url);
            return Result.success(url);
        } catch (Exception e) {
            log.error("文件上传失败：{}", e.getMessage());
        }
        return Result.error(MessageConstant.UPLOAD_FAILED);
    }
}
