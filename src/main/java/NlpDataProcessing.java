import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NlpDataProcessing {
    private static Pattern pAsr = Pattern.compile("\"asr\":\"(.*?)\"");
    private static Pattern pDomain = Pattern.compile("\"domain\":\"(.*?)\"");
    private static Pattern pIntent = Pattern.compile("\"intent\":\"(.*?)\"");
    private static Pattern pSlots = Pattern.compile("\"slots\":(.*?),\"pattern\"");

    public static void main(String[] args) throws IOException {
        Set<String> md5Set = new HashSet<>();
        FileOutputStream fos = new FileOutputStream(new File("result1.17.jsonl"));
        OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
        BufferedWriter bufferedWriter = new BufferedWriter(osw);

//        URL url = NlpDataProcessing.class.getClassLoader().getResource("nlpRequest");
//        File file = new File(url.getFile());
        FileInputStream fileInputStream = new FileInputStream("/Users/liuyang/Desktop/data/nlp/nlpRequest1.17");
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
        String line = null;
        int i = 0;
        while ((line = bufferedReader.readLine()) != null) {
            // 排除 cloud 不等于 true 的数据
            if (line.indexOf("\"cloud\":true") == -1) {
                continue;
            }

            StringBuffer writeFileBuffer = new StringBuffer();
            writeFileBuffer.append("{\"text\": \"");
            Matcher asr = pAsr.matcher(line);
            if (asr.find()) {
//                System.out.println(asr.group(1));
                writeFileBuffer.append(asr.group(1));
                writeFileBuffer.append("\", ");
            }

            writeFileBuffer.append("\"lable\": [{\"domain\": \"");
            Matcher domain = pDomain.matcher(line);
            if (domain.find()) {
//                System.out.println(domain.group(1));
                writeFileBuffer.append(domain.group(1));
                writeFileBuffer.append("\", ");
            }

            writeFileBuffer.append("\"intent\": \"");
            Matcher intent = pIntent.matcher(line);
            if (intent.find()) {
//                System.out.println(intent.group(1));
                writeFileBuffer.append(intent.group(1));
                writeFileBuffer.append("\", ");
            }

            writeFileBuffer.append("\"slots\": \"");
            Matcher slots = pSlots.matcher(line);
            if (slots.find()) {
                String data = slots.group(1);
//                System.out.println(data);
                JSONObject jsonObject = JSON.parseObject(data);
                //json对象转Map
                if (!"{}".equals(data)) {
                    Map<String, Object> map = (Map<String, Object>) jsonObject;
//                    System.out.println(map);
                    StringBuffer slotsSB = new StringBuffer();
                    for (Object value : map.values()) {
                        if (value instanceof Map) {
//                            System.out.println(value);
                            Map<String, String> map1 = (Map<String, String>) value;
                            slotsSB.append(map1.get("type"));
                            slotsSB.append(":");
                            // value 中 还嵌套一层 Map
                            if (map1.get("value").indexOf("{") != -1) {
                                JSONObject jsonObjectInner = JSON.parseObject(map1.get("value"));
                                Map<String, Object> mapInner = (Map<String, Object>) jsonObjectInner;
                                // 某些 Map 中没有 text 这个 key，却有 city 这个 key
                                if (mapInner.get("text") != null) {
                                    slotsSB.append(mapInner.get("text"));
                                } else {
                                    slotsSB.append(mapInner.get("city"));
                                }
                            } else {
                                slotsSB.append(map1.get("value").replace("\n",""));
                            }
                            slotsSB.append("|");
                        }
                    }
                    String slotsResult = slotsSB.substring(0, slotsSB.length() - 1).toString();
//                    System.out.println(slotsResult);
                    writeFileBuffer.append(slotsResult);
                } else {
                    //writeFileBuffer.append("\"\"");
                }
            }

            writeFileBuffer.append("\"}], \"asr\": {\"raw_text\": [\"\"], \"new_text\": [\"\"]}, \"type\": \"有效数据\"}");
            // 按行写入文件
            String tempString = writeFileBuffer.toString();
            String result = DigestUtils.md5Hex(tempString.getBytes(StandardCharsets.UTF_8));
            // 去重
            if (!md5Set.contains(result)) {
                md5Set.add(result);
                bufferedWriter.write(writeFileBuffer.toString());
                bufferedWriter.write("\n");
            }
        } // end while


        bufferedReader.close();
        bufferedWriter.close();
        System.out.println("程序运行结束");
    }
}
