import java.util.Map;

/**
 * 音频 的 数据结构
 */
public class AsrdataInfo {
    private String id;  // 编号
    private String speakerId;
    private String sessionId;
    private String name;  // 名字
    private Map<String, String> tags;
    private String content;  // 音频对应的识别解结果
    private String extand_info = "{\"type\": \"有效数据\"}";
    private String path;  // 路径

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSpeakerId() {
        return speakerId;
    }

    public void setSpeakerId(String speakerId) {
        this.speakerId = speakerId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getExtand_info() {
        return extand_info;
    }

    public void setExtand_info(String extand_info) {
        this.extand_info = extand_info;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}


