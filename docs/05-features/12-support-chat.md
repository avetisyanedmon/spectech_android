# 12 — Support Chat

iOS sources:
- `SpecTechIOS/Features/Support/SupportChatView.swift`
- `SpecTechIOS/Features/Support/SupportChatViewModel.swift`

A one-shot "send a message to support" form, not a real chat. The form fires
a single `POST /support/messages` and shows a success state.

## Inputs

| Field | Type | Required |
|---|---|---|
| Message | multiline text | yes |
| Order ID | text (optional reference) | no |

If the user is authenticated, the request also includes `contactInfo:
sessionStore.currentUser.phone` automatically.

## Endpoint

`POST /support/messages` (no auth required) — body:

```json
{
  "message": "string",
  "contactInfo": "string?",
  "relatedEntityType": "order" | null,
  "relatedEntityId": "string?"
}
```

If `orderIdText` is non-empty, `relatedEntityType = "order"` and
`relatedEntityId = trimmedOrderId`. Otherwise both fields are null.

## ViewModel

```kotlin
@HiltViewModel
class SupportChatViewModel @Inject constructor(
    private val supportRepo: SupportRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {

    var messageText by mutableStateOf("")
    var orderIdText by mutableStateOf("")
    var isSending by mutableStateOf(false); private set
    var sentSuccessfully by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null)

    val canSend: Boolean
        get() = messageText.trim().isNotEmpty() && !isSending

    fun send() = viewModelScope.launch {
        val trimmed = messageText.trim()
        if (trimmed.isEmpty()) return@launch
        isSending = true
        error = null
        try {
            val orderId = orderIdText.trim().takeIf { it.isNotEmpty() }
            supportRepo.sendMessage(
                message = trimmed,
                contactInfo = sessionStore.currentUser?.phone,
                relatedEntityType = orderId?.let { "order" },
                relatedEntityId = orderId,
            )
            sentSuccessfully = true
            messageText = ""
            orderIdText = ""
        } catch (e: Exception) {
            error = "Failed to send message. Please try again."
        } finally {
            isSending = false
        }
    }
}
```

## Repository

```kotlin
class SupportRepository @Inject constructor(private val api: ApiClient) {
    suspend fun sendMessage(
        message: String,
        contactInfo: String?,
        relatedEntityType: String?,
        relatedEntityId: String?,
    ): SupportMessageResponse =
        api.send<SupportMessageResponse>(
            SupportApi.SendMessage(
                SupportMessageRequest(message, contactInfo, relatedEntityType, relatedEntityId)
            )
        ).data
}
```

## UI

`ModalBottomSheet` containing:
- Header: "Contact Support"
- Optional text "Order ID" input (small, with placeholder)
- "Your message" multiline `OutlinedTextField`
- Send button (full-width, disabled when `!canSend`)
- Success state replaces the form with a checkmark + "Message sent" copy

```kotlin
@Composable
fun SupportChatSheet(onDismiss: () -> Unit, viewModel: SupportChatViewModel = hiltViewModel()) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp)) {
            if (viewModel.sentSuccessfully) {
                SuccessView(onDone = onDismiss)
            } else {
                Text(stringResource(R.string.support_title), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = viewModel.orderIdText,
                    onValueChange = { viewModel.orderIdText = it },
                    label = { Text(stringResource(R.string.support_order_id_optional)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = viewModel.messageText,
                    onValueChange = { viewModel.messageText = it },
                    label = { Text(stringResource(R.string.support_message)) },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                viewModel.error?.let { msg ->
                    Spacer(Modifier.height(8.dp))
                    Text(msg, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.send() },
                    enabled = viewModel.canSend,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (viewModel.isSending) CircularProgressIndicator(Modifier.size(20.dp))
                    else Text(stringResource(R.string.support_send))
                }
            }
        }
    }
}
```
