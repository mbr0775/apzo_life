package com.example.apzolife.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.apzolife.data.ai.AiChatTurn
import com.example.apzolife.viewmodel.ApzoViewModel

private val suggestedPrompts = listOf(
    "What should I focus on today?",
    "Why am I missing tasks?",
    "Show my pending tasks",
    "Break my next task into steps"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ApzoViewModel, onBack: () -> Unit) {
    val state by viewModel.chatState.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Ask Apzo AI", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBackIosNew, "Back") } },
                actions = {
                    // Keep the control available during generation so it can cancel
                    // a request that is stuck or taking too long.
                    if (state.messages.isNotEmpty() || state.isLoading) {
                        IconButton(
                            onClick = {
                                input = ""
                                viewModel.clearChat()
                            }
                        ) {
                            Icon(
                                imageVector = if (state.isLoading) {
                                    Icons.Rounded.Close
                                } else {
                                    Icons.Rounded.RestartAlt
                                },
                                contentDescription = if (state.isLoading) {
                                    "Cancel request"
                                } else {
                                    "Clear chat"
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(12.dp)
            ) {
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = { Text("Ask about your tasks...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        maxLines = 4,
                        enabled = !state.isLoading
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            if (input.isNotBlank() && !state.isLoading) {
                                viewModel.sendChatMessage(input)
                                input = ""
                            }
                        },
                        enabled = input.isNotBlank() && !state.isLoading,
                        modifier = Modifier.size(48.dp)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(Icons.Rounded.Send, "Send")
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (state.messages.isEmpty()) {
            EmptyChatState(
                modifier = Modifier.fillMaxSize().padding(padding),
                onPromptClick = { prompt -> viewModel.sendChatMessage(prompt) }
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.messages) { turn -> ChatBubble(turn) }
                if (state.isLoading) {
                    item { TypingBubble() }
                }
            }
        }
    }
}

/**
 * Scrollable so the keyboard opening (which shrinks available height via
 * imePadding on the bottomBar) never clips the last suggestion card - it
 * scrolls instead of getting cut off.
 */
@Composable
private fun EmptyChatState(modifier: Modifier = Modifier, onPromptClick: (String) -> Unit) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    Brush.linearGradient(listOf(Color(0xFF1D546C), Color(0xFF00B894))),
                    RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("Ask Apzo AI anything", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(6.dp))
        Text(
            "It can look at your real tasks and even create tasks or subtasks for you.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        suggestedPrompts.forEach { prompt ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp),
                onClick = { onPromptClick(prompt) }
            ) {
                Text(prompt, modifier = Modifier.padding(14.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ChatBubble(turn: AiChatTurn) {
    val isUser = turn.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    if (isUser) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(
                        topStart = 18.dp, topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (isUser) {
                Text(turn.text, fontSize = 14.sp, lineHeight = 20.sp, color = Color.White)
            } else {
                FormattedAiText(turn.text)
            }
        }
    }
}

/**
 * Renders the assistant's reply as structured chat content instead of one
 * dense paragraph: lines starting with "- " become real bullet rows, and
 * **text** becomes bold. Falls back to plain text for anything else.
 */
private const val UNICODE_BULLET = "\u2022"
private const val BROKEN_UTF8_BULLET = "\u00E2\u20AC\u00A2"
private const val DOUBLE_ENCODED_BULLET = "\u00C3\u00A2\u00E2\u0082\u00AC\u00C2\u00A2"

private data class ParsedAiLine(
    val isBullet: Boolean,
    val content: String
)

/**
 * Detects list markers without drawing a Unicode bullet as text. This avoids
 * mojibake such as "a-c" / "â€¢" when a source file or provider response is
 * decoded with the wrong character set.
 */
private fun parseAiLine(value: String): ParsedAiLine {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return ParsedAiLine(false, "")

    val prefixes = listOf(
        "- ",
        "* ",
        "$UNICODE_BULLET ",
        "$BROKEN_UTF8_BULLET ",
        "$DOUBLE_ENCODED_BULLET "
    )

    val matchedPrefix = prefixes.firstOrNull { trimmed.startsWith(it) }
    return if (matchedPrefix != null) {
        ParsedAiLine(
            isBullet = true,
            content = trimmed.removePrefix(matchedPrefix).trim()
        )
    } else {
        ParsedAiLine(isBullet = false, content = trimmed)
    }
}

/**
 * Renders list markers as a small Compose circle rather than as a text glyph.
 * Therefore the UI remains correct even when the AI returns a broken bullet.
 */
@Composable
private fun FormattedAiText(raw: String) {
    val lines = raw.trim().lines()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lines.forEach { line ->
            val parsed = parseAiLine(line)
            if (parsed.content.isEmpty()) return@forEach

            if (parsed.isBullet) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 7.dp, end = 9.dp)
                            .size(5.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                    )
                    Text(
                        text = formatInlineMarkdown(parsed.content),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Text(
                    text = formatInlineMarkdown(parsed.content),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/** Converts "**bold**" spans in [text] into a bold AnnotatedString; leaves everything else untouched. */
private fun formatInlineMarkdown(text: String) = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        val start = text.indexOf("**", i)
        if (start == -1) {
            append(text.substring(i))
            break
        }
        val end = text.indexOf("**", start + 2)
        if (end == -1) {
            append(text.substring(i))
            break
        }
        append(text.substring(i, start))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(text.substring(start + 2, end))
        }
        i = end + 2
    }
}

@Composable
private fun TypingBubble() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        }
    }
}
