package me.rerere.rikkahub.desktop

internal fun DesktopData.mapDesktopAttachments(
    transform: (DesktopAttachment) -> DesktopAttachment
): DesktopData = copy(conversations = conversations.map { it.mapDesktopAttachments(transform) })

private fun DesktopConversation.mapDesktopAttachments(
    transform: (DesktopAttachment) -> DesktopAttachment
): DesktopConversation = copy(
    messages = messages.map { it.mapDesktopAttachments(transform) },
    draftAttachments = draftAttachments.map(transform),
    branches = branches.map { branch ->
        branch.copy(messages = branch.messages.map { it.mapDesktopAttachments(transform) })
    }
)

private fun ChatMessage.mapDesktopAttachments(
    transform: (DesktopAttachment) -> DesktopAttachment
): ChatMessage = copy(attachments = attachments.map(transform))

internal fun DesktopData.attachmentBlobIds(): Set<String> = buildSet {
    conversations.forEach { conversation ->
        conversation.messages.forEach { message -> addAttachmentIds(message.attachments) }
        addAttachmentIds(conversation.draftAttachments)
        conversation.branches.forEach { branch ->
            branch.messages.forEach { message -> addAttachmentIds(message.attachments) }
        }
    }
}

private fun MutableSet<String>.addAttachmentIds(attachments: List<DesktopAttachment>) {
    attachments.forEach { attachment ->
        attachment.dataBlobId?.let(::add)
        attachment.rawDataBlobId?.let(::add)
    }
}
