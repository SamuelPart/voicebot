package com.voicebot.alo.data

import com.voicebot.alo.core.model.MessageEvent
import com.voicebot.alo.data.db.MessageEntity

fun MessageEntity.toEvent(): MessageEvent = MessageEvent(
    id = id,
    pkg = pkg,
    chatId = chatId,
    chatTitle = chatTitle,
    isGroup = isGroup,
    sender = sender,
    text = text,
    timestamp = timestamp,
    dedupKey = dedupKey,
    sbnKey = sbnKey,
    replyable = replyable,
    source = runCatching { MessageEvent.Source.valueOf(source) }.getOrDefault(MessageEvent.Source.LIVE),
)
