package com.cmacfarl.sensei.llm;

import java.util.List;
import java.util.Map;

/** A provider that turns a conversation into one structured (JSON) reply. Calls block. */
public interface LlmClient {

    /**
     * @param system   the system prompt
     * @param messages the conversation, oldest first, alternating user/model, ending with user
     * @param schema   JSON Schema the reply must follow
     */
    LlmReply send(String system, List<LlmMessage> messages, Map<String, Object> schema) throws LlmException;
}
