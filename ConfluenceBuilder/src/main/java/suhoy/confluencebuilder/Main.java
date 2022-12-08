package suhoy.confluencebuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URLEncoder;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.methods.HttpPost;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.entity.ContentType;
import org.apache.hc.core5.http.entity.EntityUtils;
import org.apache.hc.core5.http.entity.StringEntity;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.sync.CloseableHttpClient;
import org.apache.hc.client5.http.impl.sync.HttpClients;
import org.apache.hc.client5.http.methods.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.http.HttpHeaders;

/**
 *
 * @author sergei.sukhorukov
 */
public class Main {

    //хранилище аргументов
    final static Map<String, List<String>> args = new HashMap<>();
    //хранилище конфига
    final static Properties prop = new Properties();
    static String html;
    static String json;

    public static void main(final String[] arg) throws Exception {
        try {
            System.out.println("\n========== ConfluenceBuilder started ==========");

            //читаем аргументы
            ReadParams(arg);
            //читаем конфиг
            ReadProps(args.get("config").get(0));
            //читаем template html
            html = ReadTemplate(args.get("html").get(0));

            //html
            if (!args.get("desc").isEmpty()) {
                html = html.replace("${desc}", String.join(" ", args.get("desc")));
            }

            if (!args.get("build").isEmpty()) {
                html = html.replace("${build}", String.join(" ", args.get("build")));
            }

            if (!args.get("link").isEmpty()) {
                html = html.replace("${link}", String.join(" ", args.get("link")));
            }

            if (!args.get("time").isEmpty()) {
                html = html.replace("${time}", String.join(" ", args.get("time")));
            }
            //System.out.println("hrml=" + html);

            //читаем template
            json = ReadTemplate(args.get("json").get(0));
            json = json.replace("${html}", escape(html));
            json = json.replace("${title}", String.join(" ", args.get("title")));
            json = json.replace("${space}", prop.getProperty("confluence.space"));
            json = json.replace("${parent}", args.get("parent").get(0));

            //json в json object
            JSONObject jo = new JSONObject(json);
            System.out.println("\r\nJson request =\t" + jo.toString());

            HttpPost httpPost = new HttpPost(prop.getProperty("confluence.url") + prop.getProperty("confluence.page.add"));

            HttpEntity stringEntity = new StringEntity(jo.toString(), ContentType.APPLICATION_JSON);
            httpPost.setEntity(stringEntity);

            httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            httpPost.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + prop.getProperty("confluence.token"));

            CloseableHttpClient httpclient = HttpClients.createDefault();
            CloseableHttpResponse response = httpclient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            JSONObject jor = new JSONObject(EntityUtils.toString(entity, StandardCharsets.UTF_8));
            System.out.println("\r\nJson response =\t" + jor.toString() + "\r\n");

            if (response.getCode() != 200) {
                throw new Exception(jor.getString("message"));
            }

            int page = jor.getInt("id");

            //System.out.println("Page created with id =\t" + page);
            //графики
            List<String> graphs = getAllMatches(html, "<ri:attachment ri:filename=\".*\">");
            String boundary = "-------------" + System.currentTimeMillis();
            CloseableHttpClient httpclientPhoto = HttpClients.createDefault();
            HttpPost httpPostPhoto = new HttpPost(prop.getProperty("confluence.url") + prop.getProperty("confluence.attachment.add").replace("${page}", String.valueOf(page)));
            httpPostPhoto.setHeader("Content-type", "multipart/form-data; boundary=" + boundary);
            httpPostPhoto.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + prop.getProperty("confluence.token"));
            httpPostPhoto.setHeader("X-Atlassian-Token", "nocheck");

            if (!args.get("graphs").isEmpty()) {
                String graphpath = args.get("graphs").get(0);
                for (String graph : graphs) {
                    try {
                        String name = graph.split("\"")[1];
                        String path = graphpath + "/" + name;

                        //MultipartEntityBuilder multipartEntity = MultipartEntityBuilder.create()
                        HttpEntity entityPhoto = MultipartEntityBuilder.create()
                                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                                .setBoundary(boundary)
                                .addTextBody("minorEdit", "true")
                                .addBinaryBody("file", new File(path), ContentType.APPLICATION_OCTET_STREAM, name)
                                .build();

//1
                        //multipartEntity.addPart("file", new FileBody(new File(path)));
//2
                        //multipartEntity.addBinaryBody("file", new File(path), ContentType.APPLICATION_OCTET_STREAM, name);
//3
                        //InputStream inputStream = new FileInputStream(new File(path));
                        //byte[] data = IOUtils.toByteArray(inputStream);
                        //multipartEntity.addBinaryBody("file", data);
//4
                        //InputStream inputStream = new FileInputStream(new File(path));
                        //byte[] data = IOUtils.toByteArray(inputStream);
                        //InputStreamBody inputStreamBody = new InputStreamBody(new ByteArrayInputStream(data), name);
                        //multipartEntity.addPart("file", inputStreamBody);
                        //HttpEntity entityPhoto = multipartEntity.build();
                        httpPostPhoto.setEntity(entityPhoto);
                        HttpResponse responsePhoto = httpclientPhoto.execute(httpPostPhoto);

                        System.out.println("Sending " + name + " : " + httpPostPhoto.getRequestLine());
                        System.out.println("Response : " + responsePhoto.getStatusLine() + " " + EntityUtils.toString(responsePhoto.getEntity(), StandardCharsets.UTF_8));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
            System.out.println("Page created: " + prop.getProperty("confluence.url") + "/display/" + prop.getProperty("confluence.space") + "/" + URLEncoder.encode((String.join(" ", args.get("title"))), StandardCharsets.UTF_8.toString()));
            System.out.println("\n========== ConfluenceBuilder finished ==========");
            System.exit(0);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("\n========== ConfluenceBuilder finished ==========");
            System.exit(1);
        }

    }

    //чтение конфига
    public static void ReadProps(String config) {
        try {
            prop.load(new InputStreamReader(new FileInputStream(config), StandardCharsets.UTF_8));
            System.out.println("\nGot config, unsorted:");
            Enumeration keys = prop.keys();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                String value = (String) prop.get(key);
                System.out.println(key + ": " + value);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    //чтение html
    public static String ReadTemplate(String Path) throws Exception {
        try {
            Reader in = new InputStreamReader(new FileInputStream(Path), StandardCharsets.UTF_8);
            return IOUtils.toString(in);
            // System.out.println("\nHtml: " + html);
        } catch (Exception ex) {
            ex.getMessage();
            throw new Exception(ex.getMessage());
        }
    }

    //чтение аргументов
    public static void ReadParams(String[] arg) {
        List<String> options = null;
        for (int i = 0; i < arg.length; i++) {
            final String a = arg[i];

            if (a.charAt(0) == '-') {
                if (a.length() < 2) {
                    System.err.println("Error at argument " + a);
                    return;
                }

                options = new ArrayList<>();
                args.put(a.substring(1), options);
            } else if (options != null) {
                options.add(a);
            } else {
                System.err.println("Illegal parameter usage");
                return;
            }
        }
        System.out.println("\nStarted with args:");
        for (Map.Entry<String, List<String>> entry : args.entrySet()) {
            System.out.println(entry.getKey() + ":" + entry.getValue());
        }
    }

    public static List<String> getAllMatches(String text, String regex) {
        List<String> matches = new ArrayList<String>();
        Matcher m = Pattern.compile("(?=(" + regex + "))").matcher(text);
        while (m.find()) {
            matches.add(m.group(1));
        }
        return matches;
    }

    private static String escape(String raw) {
        String escaped = raw;
        escaped = escaped.replace("\\", "\\\\");
        escaped = escaped.replace("\"", "\\\"");
        escaped = escaped.replace("\b", "\\b");
        escaped = escaped.replace("\f", "\\f");
        escaped = escaped.replace("\n", "\\n");
        escaped = escaped.replace("\r", "\\r");
        escaped = escaped.replace("\t", "\\t");
        // TODO: escape other non-printing characters using uXXXX notation
        return escaped;
    }

}
