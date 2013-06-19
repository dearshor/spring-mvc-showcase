package org.springframework.samples.mvc.fileupload;

import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.NOT_MODIFIED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.IMAGE_JPEG;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mvc.extensions.ajax.AjaxUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/fileupload")
public class FileUploadController extends DirectoryWalker<Map<String, Object>> {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private File destDir;
	private String urlPrefix;

	private String delUrlPrefix;


	public FileUploadController() {
		super();
	}
	
	@PostConstruct
	public void init() {
		destDir = new File(new StringBuilder(System.getProperty("user.home"))
		.append(File.separator).append("tmp").append(File.separator)
		.append("jquery-file-upload").append(File.separator)
		.append("upload").toString());
		if (!destDir.exists()) {
			destDir.mkdirs();
		}
		
	}

	@ModelAttribute
	public void ajaxAttribute(WebRequest request, Model model) {
		model.addAttribute("ajaxRequest", AjaxUtils.isAjaxRequest(request));
	}
	
	@RequestMapping(value = "/image", method = GET)
	public ResponseEntity<byte[]> image(@RequestParam String fileName, WebRequest request) throws IOException {
		logger.debug("fileName: {}", fileName);
		long lastModified = lastModifiedOf(fileName);
		if (request.checkNotModified(lastModified)) {
			return new ResponseEntity<>(NOT_MODIFIED);
		}
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(IMAGE_JPEG);
		headers.setContentDispositionFormData("imageFile", fileName);
		return new ResponseEntity<>(imageFileData(fileName), headers, OK);
	}

	private long lastModifiedOf(String fileName) {
		File destFile = new File(destDir, fileName);
		return destFile.lastModified();
	}

	private byte[] imageFileData(String fileName) throws IOException {
		// TODO Auto-generated method stub
		File destFile = new File(destDir, fileName);
		if (!destFile.exists()) {
			throw new FileNotFoundException(fileName);
		}
		return FileUtils.readFileToByteArray(destFile);
	}

	@RequestMapping(method=RequestMethod.GET)
	public void fileUploadForm() {
	}

	@RequestMapping(method=RequestMethod.POST)
	public void processUpload(@RequestParam MultipartFile file, Model model) throws IOException {
		model.addAttribute("message", "File '" + file.getOriginalFilename() + "' uploaded successfully");
	}
	
	@RequestMapping(value = {"/2","/3"},method=RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<Map<String,List<Map<String, Object>>>> fileList(HttpServletRequest request) throws IOException {
		buildUrl(request);
		logger.debug("urlPrefix: {}; delUrlPrefix: {}", urlPrefix, delUrlPrefix);
		
		Map<String,List<Map<String, Object>>> body = new LinkedHashMap<>();
		List<Map<String, Object>> fileList = new ArrayList<>();
		walk(destDir,fileList);
		body.put("files", fileList);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(APPLICATION_JSON);
		
		return new ResponseEntity<>(body, headers, HttpStatus.OK);
	}

	private void buildUrl(HttpServletRequest request) {
		urlPrefix = request.getContextPath() + "/fileupload/image?fileName=";
		delUrlPrefix = request.getContextPath() + "/fileupload/delete?fileName=";
	}
	
	@Override
	protected void handleFile(File file, int depth,
			Collection<Map<String, Object>> results) throws IOException {
		// TODO Auto-generated method stub
		Map<String, Object> fileInfo = new LinkedHashMap<>();
		fileInfo.put("name", file.getName());
		fileInfo.put("size", FileUtils.sizeOf(file));
		fileInfo.put("url", urlPrefix + file.getName());
		fileInfo.put("thumbnail_url", "#");
		fileInfo.put("delete_url", delUrlPrefix + file.getName());
		fileInfo.put("delete_type", "DELETE");
		results.add(fileInfo);
	}

	@RequestMapping(value="/2", method=RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<Map<String,List<Map<String, Object>>>> processUpload2(
						@RequestParam MultipartFile file, Model model, HttpServletRequest request) throws IOException {
		buildUrl(request);
		File distFile = new File(destDir, file.getOriginalFilename());
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
		fileInfo.put("url", urlPrefix + file.getOriginalFilename());
		fileInfo.put("thumbnail_url", "#");
		fileInfo.put("delete_url", delUrlPrefix + file.getOriginalFilename());
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
			@RequestPart("file") Part file, HttpServletRequest request) throws IOException {
		buildUrl(request);
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
		File distFile = new File(destDir, uploadedFileName);
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
		fileInfo.put("url", urlPrefix + uploadedFileName);
		fileInfo.put("thumbnail_url", "#");
		fileInfo.put("delete_url", delUrlPrefix + uploadedFileName);
		fileInfo.put("delete_type", "DELETE");
		fileList.add(fileInfo);
		body.put("files", fileList);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(APPLICATION_JSON);
		
		return new ResponseEntity<>(body, headers, HttpStatus.CREATED);
	}
	
	@RequestMapping(value = "/delete" , method = {DELETE}) 
	@ResponseBody 
	public ResponseEntity<List<Map<String, Object>>> delete(@RequestParam String fileName) {
		logger.debug("fileName: {}", fileName);
		File file = new File(destDir, fileName);
		boolean deleted = file.delete();
		Map<String, Object> success = new LinkedHashMap<>();
		success.put("success", deleted);
		List<Map<String, Object>> results = new ArrayList<>();
		results.add(success);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(APPLICATION_JSON);
		HttpStatus statusCode;
		if (deleted) {
			statusCode = OK;
		} else {
			statusCode = ACCEPTED;
		}
		return new ResponseEntity<>(results, headers, statusCode);

	}
	
}
