package com.example.demo.controller;


import com.example.demo.dto.DownloadDTO;
import com.example.demo.utils.R;
import com.example.demo.vo.FileVO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author: zPlus
 * @date: 2023/2/19 10:38
 */
@CrossOrigin
@RestController
@RequestMapping("/download")
public class DownLoadFileController {

    @PostMapping("/getFileInfo")
    public ResponseEntity<FileVO> getFileInfo(@RequestBody DownloadDTO downloadDTO) {
        File file = new File(downloadDTO.getUploadPath());

        FileVO fileVO = new FileVO();
        fileVO.setSize(file.length());
        fileVO.setFileName(file.getName());
        return R.ok(fileVO);
    }

    @PostMapping("/file")
    public void downloadFile(HttpServletRequest request, HttpServletResponse response, @RequestBody DownloadDTO downloadDTO) throws IOException {
        File file = new File(downloadDTO.getUploadPath());
        long startByte = 0;
        long endByte = file.length() - 1;

        String range = request.getHeader("Range");
        if (range != null && range.startsWith("bytes=")) {
            String[] rangeValues = range.substring(6).split("-");
            try {
                startByte = Long.parseLong(rangeValues[0]);
                if (rangeValues.length > 1) {
                    endByte = Long.parseLong(rangeValues[1]);
                }
            } catch (NumberFormatException e) {
                startByte = 0;
                endByte = file.length() - 1;
            }
        }
        long contentLength = endByte - startByte + 1;
        //随机读取文件流
        try (RandomAccessFile accessFile = new RandomAccessFile(file, "r")) {
            //设置偏移量，从startByte
            accessFile.seek(startByte);

            response.setHeader("Accept-Ranges", "bytes");
            if (range != null) {
                if (startByte >= file.length() || startByte > endByte || endByte > file.length()) {
                    //错误请求
                    response.setHeader("Content-Range", "bytes */" + file.length());
                    response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                    response.setContentLength(0);
                    return;
                }

                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                response.setContentType("application/octet-stream");
                response.setHeader("Content-Range", "bytes " + startByte + "-" + endByte + "/" + file.length());
                response.setHeader("Content-Length", String.valueOf(contentLength));
            } else {
                //不分片时，全部返回
                response.setContentType("application/octet-stream");
                response.setHeader("Content-Length", String.valueOf(file.length()));
            }
            response.setHeader("Content-Disposition", "attachment; filename=" + file.getName());
            //浏览器不做缓存
            response.setHeader("Cache-Control", "no-cache");

            byte[] buffer = new byte[1024 * 1000];
            int readCount;
            ServletOutputStream outputStream = response.getOutputStream();
            while ((readCount = accessFile.read(buffer)) != -1) {
                outputStream.write(buffer, 0, readCount);
            }
            outputStream.flush();
        }
    }
}
