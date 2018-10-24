
import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gstm.tab.batch.UncatchExceptionHandler;
import gstm.tab.dto.DocInfoDto;

/**
 * Wi-Fi HTTP
 
 */
public class WiFi_HttpClient {

    private Context con;
    private UncatchExceptionHandler ueh = new UncatchExceptionHandler();



    public static final String CONTEXT_KOJI_PATH = "/x/xxx/doXXXX";

    /**
     * file path
     */
    public static final String KJ_MID_PATH = "/PDF/XXXX/";

    public static final String KJ_END_PATH = "/Temp/";

    /**
     */
    public static final int AP_TIME_OUT = 18000000;
    /**
     */
    public static final String ENCODING = "UTF-8";

    private static final String DOUBLE_HYPHEN = "--";
    private static final String CRLF = "\r\n";
    private static final String BOUNDARY = "*****b*o*u*n*d*a*r*y*****";

    public WiFi_HttpClient(Context con) {
        this.con = con;
    }

    /**
     * upload
     *
     * @return
     */
    public int fileUpload(File file, String userId, String no, String hisNo, String fileSaveName,List<InfoDto> infoList) {
        int isSuccess = 0;
        String result = "";

        DataOutputStream dos = null;
        FileInputStream fis = null;
        InputStream is = null;
        int status = 200;

        //upload用action
        String urlServer = SERVER_URL + CONTEXT_PATH;

        String EEEEENO = String.format("%1$010d", Integer.parseInt(EEEEE));
        String fileSaveServer = MID_PATH + AAAAA + END_PATH;

        if (!file.exists()) {
            return -2;
        }

        try {

            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            int maxBufferSize = 1024;

            URL url = new URL(urlServer);

            // https対応
            if ("https".equalsIgnoreCase(url.getProtocol())) {
                //  TrustManager Install
                TrustAllSSLConnect.trustAllHosts();
            }

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setConnectTimeout(AP_TIME_OUT);
            conn.setReadTimeout(AP_TIME_OUT);
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Charset", ENCODING);
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + BOUNDARY);
            conn.connect();

            dos = new DataOutputStream(conn.getOutputStream());

            // params
            writePapram(dos, "SSS", "SS");

            writePapram(dos, "CCC", "CC");

            writePapram(dos, "serverPath", fileSaveServer);

            writePapram(dos, "userId", userId);

            writePapram(dos, "XXX", XXX);

            writePapram(dos, "lereNo", lereNo);

            // INFO
            JSONObject json = new JSONObject();
            String delimiter = "||";

            for (DocInfoDto d : docInfoList) {
                File dFile = new File(d.getPhy_file_nm());

                if (dFile.exists()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(d.XXX);
                    sb.append(delimiter);
                    sb.append(d.XXX);
                    sb.append(delimiter);
                    sb.append(d.XXX);
                    sb.append(delimiter);
                    sb.append(d.XXXXX);
                    sb.append(delimiter);
                    sb.append(d.XXX);
                    sb.append(delimiter);
                    sb.append(d.XXX);

                    json.put(d.XXXXX, sb.toString());
                }
            }

            String jsonString = json.toString();
            writePapram(dos, "docFilesInfo", jsonString);

            //PDF para
            dos.writeBytes(DOUBLE_HYPHEN + BOUNDARY + CRLF);
            dos.writeBytes("Content-Disposition: form-data; name=\"uploadFile\";filename=\"" + fileSaveName + "\"" + CRLF);
            dos.writeBytes(CRLF);

            //PDF data
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

            //data 
            int i = 0;
            for (DocInfoDto docInfo : docInfoList) {
                File docFile = new File(docInfo.getPhy_file_nm());

                //para
                dos.writeBytes(DOUBLE_HYPHEN + BOUNDARY + CRLF);
                dos.writeBytes("Content-Disposition: form-data; name=\"uploadFiles[" + i + "]\";filename=\"" + docFile.getName() +  "\"" + CRLF);
                dos.writeBytes(CRLF);

                //data
                fis = new FileInputStream(docFile);
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

                i++;
            }

            //EOF
            dos.writeBytes(DOUBLE_HYPHEN + BOUNDARY + DOUBLE_HYPHEN + CRLF);

            status = conn.getResponseCode();

            if (status == HttpURLConnection.HTTP_NOT_FOUND) {
                return -2;
            } else if (status != HttpURLConnection.HTTP_OK) {
                return -1;
            }

            // response
            is = (InputStream) conn.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, ENCODING), 8);
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
        String[] resTmp = result.split("<br>");
        String resultCd;
        if (resTmp.length >= 2) {
            resultCd = resTmp[1];
        } else {
            resultCd = "100";
        }

        if ("XX".equals(resultCd)) {

            isSuccess = -1;
        } else if (!("AA".equals(resultCd))) {
            isSuccess = -1;
        }

        return isSuccess;
    }

    /**
     * HTTP Post
     *
     * @param dos
     * @param key
     * @param val
     */
    private void writePapram(DataOutputStream dos, String key, String val) throws IOException {
        dos.writeBytes(DOUBLE_HYPHEN + BOUNDARY + CRLF);
        dos.writeBytes("Content-Disposition: form-data; name=\"" + key + "\"" + CRLF);
        dos.writeBytes(CRLF);
        // 
        dos.write(val.getBytes(ENCODING));
        dos.writeBytes(CRLF);
    }
}


----------------------------------
// Server
	private String dfileUpload() throws IOException, IllegalArgumentException {
		FormFile[] dFiles = form.uploadFiles;

		if (dFiles != null && dFiles.length > 0){
			for (FormFile dfile : dFiles) {
				// 
				String savePath;
				if (!form.serverPath.isEmpty() && form.serverPath.endsWith("CCC/")) {
					savePath = form.serverPath.substring(0, form.serverPath.length() - 5);
				} else {
					throw new IllegalArgumentException("faild");
				}
				
				if (dfile.getFileName().toLowerCase().endsWith("aaa")) {
					savePath += "AAA/";
					
				} else if (dfile.getFileName().toLowerCase().endsWith("bbb")) {
					savePath += "BBB/";
				} else {
					throw new IllegalArgumentException("faild");
				}
				//
				savePath += dfile.getFileName();
				savePath = application.getRealPath(savePath);
				
				uploadProcess(dfile, savePath);

				filePathList.add(savePath);
			}
			
			form.uploadFiles = null;
		}
		
		// 
		return "1";
	}
	

	
	 */
	private void uploadProcess(FormFile file, String path) throws IOException {
		File fFile = new File(path);
		
		// 
		File fDir = fFile.getParentFile();

		// 
		boolean mkDirRes = true;
		if (!fDir.exists()) {
			mkDirRes = fDir.mkdirs();
		}

		if (!mkDirRes) {
			throw new IOException("faild");
		}

		OutputStream out = new BufferedOutputStream(new FileOutputStream(
				path));
		try {
			out.write(file.getFileData(), 0, file.getFileSize());
		} finally {
			out.close();
		}
	}