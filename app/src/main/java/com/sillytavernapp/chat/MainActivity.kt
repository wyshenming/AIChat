package com.sillytavernapp.chat

import android.app.Application
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.util.UUID
import java.util.zip.InflaterInputStream

private val Blue = Color(0xFF0058BE)
private val LightBlue = Color(0xFFE6EEFF)
private val Surface = Color(0xFFF8F9FF)
private val CardWhite = Color.White
private val TextMain = Color(0xFF121C2A)
private val TextMuted = Color(0xFF424754)
private val Danger = Color(0xFFBA1A1A)

data class AiCharacter(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val role: String = "AI 角色",
    val description: String = "",
    val personality: String = "",
    val scenario: String = "",
    val firstMessage: String = "你好，我在。想聊点什么？"
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val characterId: String,
    val content: String,
    val isUser: Boolean,
    val time: String = "刚刚"
)

data class ApiConfig(
    val baseUrl: String = "https://api.openai.com",
    val apiKey: String = "",
    val model: String = "gpt-4o-mini"
)

data class UserProfile(
    val name: String = "旅行者",
    val age: String = "28",
    val email: String = "user@example.com",
    val location: String = "未设置",
    val role: String = "AI 聊天玩家"
)

enum class MainTab(val label: String) {
    Chats("聊天"),
    Characters("角色"),
    Me("我的")
}

sealed class Screen {
    data object Main : Screen()
    data class ChatRoom(val characterId: String) : Screen()
    data object ProfileEditor : Screen()
    data object ApiManagement : Screen()
}

data class UiState(
    val tab: MainTab = MainTab.Chats,
    val screen: Screen = Screen.Main,
    val characters: List<AiCharacter> = sampleCharacters(),
    val messages: List<ChatMessage> = sampleMessages(),
    val apiConfig: ApiConfig = ApiConfig(),
    val profile: UserProfile = UserProfile(),
    val status: String? = null,
    val isSending: Boolean = false
)

class MainActivity : ComponentActivity() {
    private val viewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatTheme {
                AppRoot(viewModel)
            }
        }
    }
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("chatpulse_state", Application.MODE_PRIVATE)
    private val client = OkHttpClient()
    private val _ui = MutableStateFlow(loadState())
    val ui: StateFlow<UiState> = _ui

    fun selectTab(tab: MainTab) = _ui.update { it.copy(tab = tab, screen = Screen.Main) }
    fun openChat(characterId: String) = _ui.update { it.copy(screen = Screen.ChatRoom(characterId)) }
    fun openProfileEditor() = _ui.update { it.copy(screen = Screen.ProfileEditor) }
    fun openApiManagement() = _ui.update { it.copy(screen = Screen.ApiManagement) }
    fun back() = _ui.update { it.copy(screen = Screen.Main) }
    fun clearStatus() = _ui.update { it.copy(status = null) }

    fun saveApi(config: ApiConfig) {
        _ui.update { it.copy(apiConfig = config, status = "API 配置已保存") }
        persist()
    }

    fun saveProfile(profile: UserProfile) {
        _ui.update { it.copy(profile = profile, status = "个人资料已保存") }
        persist()
    }

    fun deleteCharacter(id: String) {
        _ui.update { state ->
            val remaining = state.characters.filterNot { it.id == id }
            state.copy(characters = remaining, messages = state.messages.filterNot { it.characterId == id })
        }
        persist()
    }

    fun createBlankCharacter() {
        val character = AiCharacter(
            name = "新角色",
            role = "自定义",
            description = "点击编辑按钮前，可先用 JSON 或酒馆 PNG 角色卡导入完整设定。",
            firstMessage = "你好，我是新角色。"
        )
        _ui.update { it.copy(characters = listOf(character) + it.characters, status = "已创建空白角色") }
        persist()
    }

    fun importCharacter(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val bytes = getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("无法读取文件")
                parseCharacter(bytes, uri.toString())
            }.onSuccess { character ->
                _ui.update { it.copy(characters = listOf(character) + it.characters, status = "已导入角色：${character.name}") }
                persist()
            }.onFailure { error ->
                _ui.update { it.copy(status = "导入失败：${error.message ?: "格式不支持"}") }
            }
        }
    }

    fun sendMessage(characterId: String, text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val userMessage = ChatMessage(characterId = characterId, content = clean, isUser = true)
        _ui.update { it.copy(messages = it.messages + userMessage, isSending = true) }
        persist()

        viewModelScope.launch(Dispatchers.IO) {
            val state = _ui.value
            val character = state.characters.firstOrNull { it.id == characterId }
            val config = state.apiConfig
            if (config.apiKey.isBlank()) {
                appendAssistant(characterId, "请先在“我的 > API 导入”中填写 OpenAI 兼容 API Key。")
                return@launch
            }
            val reply = runCatching {
                requestOpenAiCompatibleReply(config, character, state.messages.filter { it.characterId == characterId } + userMessage)
            }.getOrElse { "请求失败：${it.message ?: "请检查 Base URL、Key 和网络"}" }
            appendAssistant(characterId, reply)
        }
    }

    private fun appendAssistant(characterId: String, text: String) {
        _ui.update {
            it.copy(
                messages = it.messages + ChatMessage(characterId = characterId, content = text, isUser = false),
                isSending = false
            )
        }
        persist()
    }

    private fun requestOpenAiCompatibleReply(
        config: ApiConfig,
        character: AiCharacter?,
        messages: List<ChatMessage>
    ): String {
        val base = config.baseUrl.trim().trimEnd('/')
        val system = buildString {
            append("你是一个 AI 聊天角色。请全程使用中文自然回复。")
            if (character != null) {
                append("\n角色名：${character.name}")
                append("\n定位：${character.role}")
                append("\n简介：${character.description}")
                append("\n性格：${character.personality}")
                append("\n场景：${character.scenario}")
            }
        }
        val jsonMessages = JSONArray().put(JSONObject().put("role", "system").put("content", system))
        messages.takeLast(20).forEach {
            jsonMessages.put(
                JSONObject()
                    .put("role", if (it.isUser) "user" else "assistant")
                    .put("content", it.content)
            )
        }
        val body = JSONObject()
            .put("model", config.model)
            .put("messages", jsonMessages)
            .put("temperature", 0.8)
            .toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("$base/v1/chat/completions")
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("HTTP ${response.code}: ${raw.take(160)}")
            return JSONObject(raw)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
        }
    }

    private fun loadState(): UiState {
        val saved = prefs.getString("state", null) ?: return UiState()
        return runCatching {
            val json = JSONObject(saved)
            UiState(
                characters = json.getJSONArray("characters").toCharacterList(),
                messages = json.getJSONArray("messages").toMessageList(),
                apiConfig = json.getJSONObject("api").toApiConfig(),
                profile = json.getJSONObject("profile").toProfile()
            )
        }.getOrDefault(UiState())
    }

    private fun persist() {
        val state = _ui.value
        val json = JSONObject()
            .put("characters", JSONArray().also { array -> state.characters.forEach { array.put(it.toJson()) } })
            .put("messages", JSONArray().also { array -> state.messages.forEach { array.put(it.toJson()) } })
            .put("api", state.apiConfig.toJson())
            .put("profile", state.profile.toJson())
        prefs.edit().putString("state", json.toString()).apply()
    }
}

@Composable
fun ChatTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.lightColorScheme(
            primary = Blue,
            secondary = Color(0xFF0060AC),
            background = Surface,
            surface = Surface,
            onSurface = TextMain,
            error = Danger
        ),
        content = content
    )
}

@Composable
fun AppRoot(viewModel: ChatViewModel) {
    val state by viewModel.ui.collectAsState()
    BackHandler(enabled = state.screen != Screen.Main) {
        viewModel.back()
    }
    Surface(Modifier.fillMaxSize(), color = Surface) {
        when (val screen = state.screen) {
            Screen.Main -> MainScaffold(state, viewModel)
            is Screen.ChatRoom -> ChatRoomScreen(state, screen.characterId, viewModel)
            Screen.ProfileEditor -> ProfileEditorScreen(state.profile, viewModel)
            Screen.ApiManagement -> ApiManagementScreen(state.apiConfig, viewModel)
        }
        state.status?.let {
            StatusDialog(it, onDismiss = viewModel::clearStatus)
        }
    }
}

@Composable
fun StatusDialog(text: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("知道了") } },
        title = { Text("提示") },
        text = { Text(text) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(state: UiState, viewModel: ChatViewModel) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importCharacter(uri)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.tab == MainTab.Me) "酒馆 AI" else state.tab.label, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface, titleContentColor = Blue),
                actions = {
                    if (state.tab == MainTab.Characters) {
                        TextButton(onClick = { picker.launch(arrayOf("application/json", "image/png", "*/*")) }) {
                            Text("导入")
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = CardWhite) {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = state.tab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        icon = { Text(tabSymbol(tab)) },
                        label = { Text(tab.label) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (state.tab == MainTab.Characters) {
                FloatingActionButton(containerColor = Blue, contentColor = Color.White, onClick = viewModel::createBlankCharacter) {
                    Text("新建")
                }
            }
        }
    ) { padding ->
        when (state.tab) {
            MainTab.Chats -> ChatsScreen(state, viewModel, padding)
            MainTab.Characters -> CharactersScreen(state, viewModel, padding)
            MainTab.Me -> MeScreen(state, viewModel, padding)
        }
    }
}

@Composable
fun ChatsScreen(state: UiState, viewModel: ChatViewModel, padding: PaddingValues) {
    LazyColumn(
        modifier = Modifier.padding(padding).fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("最近消息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                state.characters.take(4).forEach { AvatarChip(it.name.take(2)) }
            }
        }
        items(state.characters) { character ->
            val last = state.messages.lastOrNull { it.characterId == character.id }
            ChatListCard(character, last, onClick = { viewModel.openChat(character.id) })
        }
    }
}

@Composable
fun ChatListCard(character: AiCharacter, last: ChatMessage?, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(character.name.take(2), 56)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(character.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(last?.time ?: "未开始", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                }
                Text(
                    last?.content ?: character.firstMessage,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CharactersScreen(state: UiState, viewModel: ChatViewModel, padding: PaddingValues) {
    var keyword by remember { mutableStateOf("") }
    val characters = state.characters.filter {
        it.name.contains(keyword, true) || it.description.contains(keyword, true) || it.role.contains(keyword, true)
    }
    LazyColumn(
        modifier = Modifier.padding(padding).fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                placeholder = { Text("搜索角色...") },
                singleLine = true
            )
        }
        items(characters) { character ->
            CharacterCard(
                character = character,
                onChat = { viewModel.openChat(character.id) },
                onDelete = { viewModel.deleteCharacter(character.id) }
            )
        }
        item {
            OutlinedButton(
                onClick = viewModel::createBlankCharacter,
                modifier = Modifier.fillMaxWidth().height(72.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("创建角色")
            }
        }
    }
}

@Composable
fun CharacterCard(character: AiCharacter, onChat: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onChat),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(character.name.take(2), 56)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(character.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    AssistChip(onClick = {}, label = { Text(character.role) })
                }
                TextButton(onClick = onDelete) { Text("删除", color = Danger) }
            }
            Text(character.description.ifBlank { "暂无角色简介" }, color = TextMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun MeScreen(state: UiState, viewModel: ChatViewModel, padding: PaddingValues) {
    LazyColumn(
        modifier = Modifier.padding(padding).fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Avatar(state.profile.name.take(2), 96)
            Spacer(Modifier.height(12.dp))
            Text(state.profile.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("高级会员 · ${state.profile.email}", color = TextMuted)
        }
        item { MenuCard("API 导入", "管理 OpenAI 兼容接口、Key 和模型", onClick = viewModel::openApiManagement) }
        item { MenuCard("我的资料", "昵称、年龄、自定义资料字段", onClick = viewModel::openProfileEditor) }
        item { SettingsPanel() }
    }
}

@Composable
fun MenuCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, color = TextMuted)
            }
            Text("进入", color = Blue, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SettingsPanel() {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = LightBlue)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("快捷设置", color = TextMuted, style = MaterialTheme.typography.labelMedium)
            Text("通知偏好")
            Text("隐私与数据")
            Text("退出登录", color = Danger)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(state: UiState, characterId: String, viewModel: ChatViewModel) {
    val character = state.characters.firstOrNull { it.id == characterId } ?: return
    var input by remember { mutableStateOf("") }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(character.name, fontWeight = FontWeight.Bold)
                        Text("在线", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                },
                navigationIcon = { TextButton(onClick = viewModel::back) { Text("返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.background(CardWhite).padding(12.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f).heightIn(min = 56.dp, max = 132.dp),
                    shape = RoundedCornerShape(18.dp),
                    placeholder = { Text("输入消息...") }
                )
                Button(
                    onClick = {
                        viewModel.sendMessage(characterId, input)
                        input = ""
                    },
                    enabled = !state.isSending
                ) {
                    Text(if (state.isSending) "发送中" else "发送")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    AssistChip(onClick = {}, label = { Text("今天") })
                }
            }
            items(state.messages.filter { it.characterId == characterId }) { message ->
                MessageBubble(message)
            }
            if (state.isSending) {
                item { Text("${character.name} 正在思考...", color = TextMuted) }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .background(
                        if (message.isUser) Blue else CardWhite,
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (message.isUser) 18.dp else 4.dp,
                            bottomEnd = if (message.isUser) 4.dp else 18.dp
                        )
                    )
                    .padding(14.dp)
            ) {
                Text(message.content, color = if (message.isUser) Color.White else TextMain)
            }
            Text(message.time, color = TextMuted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditorScreen(profile: UserProfile, viewModel: ChatViewModel) {
    var name by remember { mutableStateOf(profile.name) }
    var age by remember { mutableStateOf(profile.age) }
    var email by remember { mutableStateOf(profile.email) }
    var location by remember { mutableStateOf(profile.location) }
    var role by remember { mutableStateOf(profile.role) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑资料") },
                navigationIcon = { TextButton(onClick = viewModel::back) { Text("返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        },
        bottomBar = {
            Button(
                onClick = { viewModel.saveProfile(UserProfile(name, age, email, location, role)) },
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue)
            ) {
                Text("保存修改")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Avatar(name.take(2), 120)
                Spacer(Modifier.height(12.dp))
                Text(name.ifBlank { "未命名用户" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            item { LabeledField("昵称", name) { name = it } }
            item { LabeledField("年龄", age) { age = it } }
            item { LabeledField("邮箱", email) { email = it } }
            item { LabeledField("位置", location) { location = it } }
            item { LabeledField("身份", role) { role = it } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiManagementScreen(config: ApiConfig, viewModel: ChatViewModel) {
    var baseUrl by remember { mutableStateOf(config.baseUrl) }
    var key by remember { mutableStateOf(config.apiKey) }
    var model by remember { mutableStateOf(config.model) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API 管理") },
                navigationIcon = { TextButton(onClick = viewModel::back) { Text("返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Blue)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("管理连接", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("填写 OpenAI 兼容服务地址、API Key 与模型名称。请求路径固定为 /v1/chat/completions。", color = Color.White.copy(alpha = 0.9f))
                    }
                }
            }
            item { LabeledField("Base URL", baseUrl) { baseUrl = it } }
            item { LabeledField("API Key", key) { key = it } }
            item { LabeledField("模型", model) { model = it } }
            item {
                Button(
                    onClick = { viewModel.saveApi(ApiConfig(baseUrl.trim(), key.trim(), model.trim())) },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue)
                ) {
                    Text("保存 API")
                }
            }
            item {
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = LightBlue)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("安全提示", fontWeight = FontWeight.Bold)
                        Text("Key 只保存在本机 SharedPreferences 中。正式发布前建议接入 Android Keystore 加密保存。", color = TextMuted)
                    }
                }
            }
        }
    }
}

@Composable
fun LabeledField(label: String, value: String, onChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, color = TextMuted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )
    }
}

@Composable
fun Avatar(text: String, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(LightBlue, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(text.ifBlank { "AI" }, color = Blue, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AvatarChip(text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Avatar(text, 54)
        Text(text, style = MaterialTheme.typography.labelSmall)
    }
}

private fun tabSymbol(tab: MainTab): String = when (tab) {
    MainTab.Chats -> "聊"
    MainTab.Characters -> "角"
    MainTab.Me -> "我"
}

private fun parseCharacter(bytes: ByteArray, nameHint: String): AiCharacter {
    val text = bytes.decodeToStringOrNull()
    val jsonText = if (text?.trimStart()?.startsWith("{") == true) {
        text
    } else {
        extractSillyTavernJsonFromPng(bytes)
    } ?: error("未找到 JSON 或酒馆 PNG 角色卡数据")
    return jsonToCharacter(JSONObject(jsonText), nameHint)
}

private fun jsonToCharacter(root: JSONObject, nameHint: String): AiCharacter {
    val data = root.optJSONObject("data") ?: root
    val name = data.optString("name", root.optString("name", nameHint.substringAfterLast('/'))).ifBlank { "未命名角色" }
    val description = data.optString("description", root.optString("description", ""))
    val personality = data.optString("personality", root.optString("personality", ""))
    val scenario = data.optString("scenario", root.optString("scenario", ""))
    val first = data.optString("first_mes", root.optString("first_mes", root.optString("firstMessage", "你好，我在。")))
    val role = data.optString("role", data.optString("creator_notes", "AI 角色")).lineSequence().firstOrNull()?.take(18) ?: "AI 角色"
    return AiCharacter(
        name = name,
        role = role.ifBlank { "AI 角色" },
        description = description,
        personality = personality,
        scenario = scenario,
        firstMessage = first.ifBlank { "你好，我在。" }
    )
}

private fun extractSillyTavernJsonFromPng(bytes: ByteArray): String? {
    if (bytes.size < 8 || bytes[0] != 0x89.toByte() || bytes[1] != 0x50.toByte()) return null
    val input = DataInputStream(ByteArrayInputStream(bytes))
    input.skipBytes(8)
    while (input.available() > 12) {
        val length = input.readInt()
        val typeBytes = ByteArray(4)
        input.readFully(typeBytes)
        val type = typeBytes.decodeToString()
        val data = ByteArray(length)
        input.readFully(data)
        input.skipBytes(4)
        when (type) {
            "tEXt" -> parseTextChunk(data)?.let { if (it.first == "chara" || it.first == "ccv3") return decodeEmbeddedJson(it.second) }
            "zTXt" -> parseCompressedTextChunk(data)?.let { if (it.first == "chara" || it.first == "ccv3") return decodeEmbeddedJson(it.second) }
            "iTXt" -> parseInternationalTextChunk(data)?.let { if (it.first == "chara" || it.first == "ccv3") return decodeEmbeddedJson(it.second) }
        }
    }
    return null
}

private fun parseTextChunk(data: ByteArray): Pair<String, String>? {
    val zero = data.indexOf(0)
    if (zero <= 0) return null
    return data.copyOfRange(0, zero).decodeToString() to data.copyOfRange(zero + 1, data.size).decodeToString()
}

private fun parseCompressedTextChunk(data: ByteArray): Pair<String, String>? {
    val zero = data.indexOf(0)
    if (zero <= 0 || zero + 2 >= data.size) return null
    val keyword = data.copyOfRange(0, zero).decodeToString()
    val compressed = data.copyOfRange(zero + 2, data.size)
    return keyword to InflaterInputStream(ByteArrayInputStream(compressed)).use { it.readAllBytes().decodeToString() }
}

private fun parseInternationalTextChunk(data: ByteArray): Pair<String, String>? {
    val keywordEnd = data.indexOf(0)
    if (keywordEnd <= 0 || keywordEnd + 2 >= data.size) return null
    val keyword = data.copyOfRange(0, keywordEnd).decodeToString()
    val compressed = data[keywordEnd + 1].toInt() == 1
    var offset = keywordEnd + 3
    repeat(2) {
        val end = data.indexOf(0, offset)
        if (end < 0) return null
        offset = end + 1
    }
    val textBytes = data.copyOfRange(offset, data.size)
    val text = if (compressed) InflaterInputStream(ByteArrayInputStream(textBytes)).use { it.readAllBytes().decodeToString() } else textBytes.decodeToString()
    return keyword to text
}

private fun decodeEmbeddedJson(value: String): String {
    val trimmed = value.trim()
    if (trimmed.startsWith("{")) return trimmed
    return Base64.decode(trimmed, Base64.DEFAULT).decodeToString()
}

private fun ByteArray.decodeToStringOrNull(): String? = runCatching { decodeToString() }.getOrNull()
private fun ByteArray.indexOf(value: Int, start: Int = 0): Int {
    for (i in start until size) if (this[i].toInt() == value) return i
    return -1
}

private fun JSONArray.toCharacterList(): List<AiCharacter> = List(length()) { index ->
    getJSONObject(index).let {
        AiCharacter(
            id = it.getString("id"),
            name = it.getString("name"),
            role = it.optString("role", "AI 角色"),
            description = it.optString("description"),
            personality = it.optString("personality"),
            scenario = it.optString("scenario"),
            firstMessage = it.optString("firstMessage", "你好，我在。")
        )
    }
}

private fun JSONArray.toMessageList(): List<ChatMessage> = List(length()) { index ->
    getJSONObject(index).let {
        ChatMessage(
            id = it.getString("id"),
            characterId = it.getString("characterId"),
            content = it.getString("content"),
            isUser = it.getBoolean("isUser"),
            time = it.optString("time", "刚刚")
        )
    }
}

private fun JSONObject.toApiConfig() = ApiConfig(
    baseUrl = optString("baseUrl", "https://api.openai.com"),
    apiKey = optString("apiKey", ""),
    model = optString("model", "gpt-4o-mini")
)

private fun JSONObject.toProfile() = UserProfile(
    name = optString("name", "旅行者"),
    age = optString("age", "28"),
    email = optString("email", "user@example.com"),
    location = optString("location", "未设置"),
    role = optString("role", "AI 聊天玩家")
)

private fun AiCharacter.toJson() = JSONObject()
    .put("id", id)
    .put("name", name)
    .put("role", role)
    .put("description", description)
    .put("personality", personality)
    .put("scenario", scenario)
    .put("firstMessage", firstMessage)

private fun ChatMessage.toJson() = JSONObject()
    .put("id", id)
    .put("characterId", characterId)
    .put("content", content)
    .put("isUser", isUser)
    .put("time", time)

private fun ApiConfig.toJson() = JSONObject()
    .put("baseUrl", baseUrl)
    .put("apiKey", apiKey)
    .put("model", model)

private fun UserProfile.toJson() = JSONObject()
    .put("name", name)
    .put("age", age)
    .put("email", email)
    .put("location", location)
    .put("role", role)

private fun sampleCharacters(): List<AiCharacter> = listOf(
    AiCharacter(
        id = "sample-star",
        name = "星河",
        role = "温柔向导",
        description = "擅长陪伴、整理思绪和进行轻剧情对话的 AI 角色。",
        personality = "温柔、耐心、会主动追问细节。",
        scenario = "用户进入一个安静的星舰休息舱，与星河开始对话。",
        firstMessage = "欢迎回来。今天想让我陪你聊天，还是一起整理一个故事？"
    ),
    AiCharacter(
        id = "sample-lin",
        name = "林澈",
        role = "故事搭档",
        description = "适合长篇角色扮演，会维护世界观和人物关系。",
        personality = "克制、聪明、带一点幽默。",
        scenario = "雨夜的旧书店里，林澈正等你推门而入。",
        firstMessage = "你终于来了。我刚找到一页很奇怪的手稿。"
    ),
    AiCharacter(
        id = "sample-ink",
        name = "墨蓝",
        role = "设定顾问",
        description = "帮助你创建角色卡、润色开场白、补全世界设定。",
        personality = "直接、专业、重视结构。",
        scenario = "创作工作台已打开，所有设定都可以重新编排。",
        firstMessage = "把你的角色草稿给我，我会先帮你抓住核心冲突。"
    )
)

private fun sampleMessages(): List<ChatMessage> {
    val first = sampleCharacters().first()
    return listOf(
        ChatMessage(characterId = first.id, content = first.firstMessage, isUser = false, time = "10:42")
    )
}
