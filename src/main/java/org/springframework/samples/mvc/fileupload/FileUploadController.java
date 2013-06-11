package org.springframework.samples.mvc.fileupload;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.Part;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mvc.extensions.ajax.AjaxUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/fileupload")
public class FileUploadController {

	@ModelAttribute
	public void ajaxAttribute(WebRequest request, Model model) {
		model.addAttribute("ajaxRequest", AjaxUtils.isAjaxRequest(request));
	}

	@RequestMapping(method=RequestMethod.GET)
	public void fileUploadForm() {
	}

	@RequestMapping(method=RequestMethod.POST)
	public void processUpload(@RequestParam MultipartFile file, Model model) throws IOException {
		model.addAttribute("message", "File '" + file.getOriginalFilename() + "' uploaded successfully");
	}

	@RequestMapping(value="/2",method=RequestMethod.GET)
	@ResponseBody
	public String fileUploadForm2() {
		return "fake text";
	}
	
	@RequestMapping(value="/2", method=RequestMethod.PUT)
	@ResponseBody
	public ResponseEntity<Map<String,List<Map<String, Object>>>> processUpload2(
						@RequestParam MultipartFile file, Model model) throws IOException {
		File distDir = new File(new StringBuilder(System.getProperty("user.home"))
								.append(File.separator).append("tmp").append(File.separator)
								.append("jquery-file-upload").append(File.separator)
								.append("upload").toString());
		if (!distDir.exists()) {
			distDir.mkdirs();
		}
		File distFile = new File(distDir, file.getOriginalFilename());
		BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(distFile));
		BufferedInputStream bin = new BufferedInputStream(file.getInputStream());
		byte[] bytesToProcess = new byte[2048];
		int bytesReaded;
		while ((bytesReaded = bin.read(bytesToProcess)) > -1) {
			bout.write(bytesToProcess, 0, bytesReaded);
		}
		bout.close();
		
		Map<String,List<Map<String, Object>>> body = new LinkedHashMap<>();
		List<Map<String, Object>> fileList = new ArrayList<>();
		Map<String, Object> fileInfo = new LinkedHashMap<>();
		fileInfo.put("name", file.getOriginalFilename());
		fileInfo.put("size", file.getSize());
		fileInfo.put("url", "#");
		fileInfo.put("thumbnail_url", "#");
		fileInfo.put("delete_url", "#");
		fileInfo.put("delete_type", "DELETE");
		fileList.add(fileInfo);
		body.put("files", fileList);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(APPLICATION_JSON);
		
		return new ResponseEntity<>(body, headers, HttpStatus.CREATED);
	}
	
	
	@RequestMapping(value="/3", method=RequestMethod.PUT)
	@ResponseBody
	public ResponseEntity<Map<String,List<Map<String, Object>>>> processUpload3(
			@RequestPart("file") Part file) throws IOException {
		File distDir = new File(new StringBuilder(System.getProperty("user.home"))
		.append(File.separator).append("tmp").append(File.separator)
		.append("jquery-file-upload").append(File.separator)
		.append("upload").toString());
		if (!distDir.exists()) {
			distDir.mkdirs();
		}
		
		// Finding the fileName //
        String uploadedFileName = "";
        String contentDisposition = file.getHeader("content-disposition");
        // TODO use regex to extract filename from content-disposition
		for (String temp : contentDisposition.split(";"))
        {
          if (temp.trim().startsWith("filename"))
           {
              uploadedFileName = temp.substring(temp.indexOf('=') + 1).trim().replace("\"", "");
           }
        }
		File distFile = new File(distDir, uploadedFileName);
		BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(distFile));
		InputStream bin = file.getInputStream();
		byte[] bytesToProcess = new byte[2048];
		int bytesReaded;
		int fileSize = 0;
		while ((bytesReaded = bin.read(bytesToProcess)) > -1) {
			bout.write(bytesToProcess, 0, bytesReaded);
			fileSize += bytesReaded;
		}
		bout.close();
		
		Map<String,List<Map<String, Object>>> body = new LinkedHashMap<>();
		List<Map<String, Object>> fileList = new ArrayList<>();
		Map<String, Object> fileInfo = new LinkedHashMap<>();
		fileInfo.put("name", uploadedFileName);
		fileInfo.put("size", fileSize);
		fileInfo.put("url", "#");
		fileInfo.put("thumbnail_url", "#");
		fileInfo.put("delete_url", "#");
		fileInfo.put("delete_type", "DELETE");
		fileList.add(fileInfo);
		body.put("files", fileList);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(APPLICATION_JSON);
		
		return new ResponseEntity<>(body, headers, HttpStatus.CREATED);
	}
	
	
	
}
