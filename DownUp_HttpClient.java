
import android.content.Context;

import org.apache.http.HttpStatus;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 */
public class WiFi_HttpClient {

    private Context con;
    private UncatchExceptionHandler ueh = new UncatchExceptionHandler();

    private static final String DOUBLE_HYPHEN = "--";
    private static final String CRLF = "\r\n";
    private static final String BOUNDARY = "*****b*o*u*n*d*a*r*y*****";

    public WiFi_HttpClient(Context con) {
        this.con = con;
    }

    /**
     */
    public int fileDownload(String filename, String localPath, String serverPath, String... xXX_ID) {
        int isSuccess = 0;

        OutputStream os = null;
        InputStream is = null;

        DataOutputStream dos = null;
        FileInputStream fis = null;

        //acition
        String urlServer = Constants.CONTEXT_PATH + Constants.CONTEXT_PATH;

        try {
            URL url = new URL(urlServer);

            // https
            if ("https".equalsIgnoreCase(url.getProtocol())) {
                //  TrustManage
                TrustAllSSLConnect.trustAllHosts();
            }

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setConnectTimeout(WiFi_Constants._AP_TIME_OUT);
            conn.setReadTimeout(WiFi_Constants._AP_TIME_OUT);

            dos = new DataOutputStream(conn.getOutputStream());

            // 
            dos.writeBytes("xxx=1");
            dos.writeBytes("&");
            dos.writeBytes("serverPath="+ serverPath);
            dos.writeBytes("&");
            dos.writeBytes("fileName=" + filename);

            final int status = conn.getResponseCode();

            if (status == HttpStatus.SC_NOT_FOUND) {
                is = conn.getErrorStream();
                if (is != null) {
                    BufferedReader readerErr = new BufferedReader(new InputStreamReader(is, WiFi_Constants.ENCODING), 8);
                    StringBuilder sbErr = new StringBuilder();
                    String lineErr = null;
                    while ((lineErr = readerErr.readLine()) != null) {
                        sbErr.append(lineErr);
                    }
                    String reportErr = sbErr.toString();

                    if (reportErr.contains("ERR_XXX")) {
                        return -2;
                    }
                }
                return -1;
            } else if (status != HttpStatus.SC_OK) {
                return -1;
            }

            String fileNm = filename;
            //masterFileDownload
            if (iFnm_Ins_ID != null && iFnm_Ins_ID.length != 0) {
                fileNm = filename.replace(iFnm_Ins_ID[0] + "_", "");
            }
            File localFile = new File(localPath + fileNm);
            if (localFile.exists()) {
                localFile.delete();
            }


            localFile.createNewFile();

            is = (InputStream) conn.getContent();
            os = new FileOutputStream(localFile);
            int len = 0;

            byte[] buffer = new byte[4 * 1024];
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }

            os.flush();

            // 
            fis = new FileInputStream(localFile);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis, WiFi_Constants.ENCODING), 8);
            // 
            String line = reader.readLine();
            // responsehtml
            if (line != null && line.startsWith("<!DOCTYPE")) {
                StringBuilder sb = new StringBuilder();
                sb.append(line);
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                return -1;
            }

            //  String[] fileNames = resultCode.split("<br>");
        } catch (Exception e) {
            ueh.saveErrorInfo(con, null, e);
            isSuccess = -1;
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (is != null) {
                    is.close();
                }
                if (dos != null) {
                    dos.close();
                }
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                ueh.saveErrorInfo(con, null, e);
                isSuccess = -1;
            }
        }

        return isSuccess;
    }

    /**
     * upl
     */
    public int fileUpload(File file, String server_path, boolean makeDir) {
        int isSuccess = 0;
        String result = "";

        DataOutputStream dos = null;
        FileInputStream fis = null;
        InputStream is = null;
        int status = 200;

        //action
        String urlServer = Constants.CONTEXT_PATH + Constants.CONTEXT_PATH;

        if (!file.exists()) {
            return -2;
        }

        try {

            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            int maxBufferSize = 1024;

            URL url = new URL(urlServer);

            // https
            if ("https".equalsIgnoreCase(url.getProtocol())) {
                // TrustManager
                TrustAllSSLConnect.trustAllHosts();
            }

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setConnectTimeout(WiFi_Constants._AP_TIME_OUT);
            conn.setReadTimeout(WiFi_Constants._AP_TIME_OUT);
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Charset", WiFi_Constants.ENCODING);
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + BOUNDARY);
            conn.connect();

            dos = new DataOutputStream(conn.getOutputStream());

            writePapram(dos, "xxx", "1");

            writePapram(dos, "serverPath", server_path);

            writePapram(dos, "makeDir", makeDir + "");

            dos.writeBytes(DOUBLE_HYPHEN + BOUNDARY + CRLF);
            dos.writeBytes("Content-Disposition: form-data; name=\"uploadFile\";filename=\"" + file.getName() + "\"" + CRLF);
            dos.writeBytes(CRLF);

            fis = new FileInputStream(file);
            bytesAvailable = fis.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];

            bytesRead = fis.read(buffer, 0, bufferSize);

            while (bytesRead > 0) {
                dos.write(buffer, 0, bufferSize);
                bytesAvailable = fis.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fis.read(buffer, 0, bufferSize);
            }

            dos.writeBytes(CRLF);

            //EOF
            dos.writeBytes(DOUBLE_HYPHEN + BOUNDARY + DOUBLE_HYPHEN + CRLF);

            status = conn.getResponseCode();

            if (status == HttpStatus.SC_NOT_FOUND) {
                return -2;
            } else if (status != HttpStatus.SC_OK) {
                return -1;
            }

            // response
            is = (InputStream) conn.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, WiFi_Constants.ENCODING), 8);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            result = sb.toString();

            dos.flush();
            conn.disconnect();
        } catch (Exception e) {
            ueh.saveErrorInfo(con, null, e);
            isSuccess = -1;
        } finally {
            try {
                if (dos != null) {
                    dos.close();
                }

                if (fis != null) {
                    fis.close();
                }

                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                ueh.saveErrorInfo(con, null, e);
                isSuccess = -1;
            }
        }

        // response
        String resultCd = (result.split("<br>"))[1];

        if ("xx".equals(resultCd)) {
            isSuccess = -1;
        } else if (!("00".equals(resultCd))) {
            isSuccess = -1;
        }

        return isSuccess;
    }

    /**
     * HTTP Postpara
     */
    private void writePapram(DataOutputStream dos, String key, String val) throws IOException {
        dos.writeBytes(DOUBLE_HYPHEN + BOUNDARY + CRLF);
        dos.writeBytes("Content-Disposition: form-data; name=\"" + key + "\"" + CRLF);
        dos.writeBytes(CRLF);
        dos.writeBytes(val);
        dos.writeBytes(CRLF);
    }
}


------------------------------server


private String fileDownload() throws Exception {
		//
		String serverPath = form.serverPath;
		String fileName = form.fileName;
		boolean isAllowedPath = serverPath.startsWith("/BBB/") || serverPath.startsWith("/AAA/");
		
		// 
		if (!isAllowedPath) {
			return "err";
		}
		
		String filePath = application.getRealPath(serverPath + fileName);

		File file = new File(filePath);
		
		if (!file.exists()) {
			return "err";
		}
		
		BufferedInputStream in = null;
		ServletOutputStream out = null;
		
		try{
			out = response.getOutputStream();
			in = new BufferedInputStream(new FileInputStream(file));
			response.setContentType("application/x-download");
			response.setHeader("Content-disposition", "attachment; filename=" + new String(fileName.getBytes("Shift_JIS"), "8859_1") + "");
			
			int length;
			byte buffer[] = new byte[4 * 1024];
			while ((length = in.read(buffer, 0, buffer.length)) != -1) {
				out.write(buffer, 0, length);
			}
		} finally {
			if(in != null) {
				in.close();
			}
			if(out != null) {
				out.flush();
				out.close();
			}
		}
		
		return "ok";
	}
