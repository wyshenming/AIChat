import 'dart:convert';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  runApp(const AiChatApp());
}

const blue = Color(0xFF0058BE);
const lightBlue = Color(0xFFE6EEFF);
const bg = Color(0xFFF8F9FF);
const textMain = Color(0xFF121C2A);
const textMuted = Color(0xFF727785);
const danger = Color(0xFFBA1A1A);

class AiCharacter {
  AiCharacter({
    required this.id,
    required this.name,
    required this.role,
    required this.description,
    required this.firstMessage,
  });

  final String id;
  final String name;
  final String role;
  final String description;
  final String firstMessage;

  AiCharacter copyWith({
    String? id,
    String? name,
    String? role,
    String? description,
    String? firstMessage,
  }) =>
      AiCharacter(
        id: id ?? this.id,
        name: name ?? this.name,
        role: role ?? this.role,
        description: description ?? this.description,
        firstMessage: firstMessage ?? this.firstMessage,
      );

  Map<String, dynamic> toJson() => {
        'id': id,
        'name': name,
        'role': role,
        'description': description,
        'firstMessage': firstMessage,
      };

  factory AiCharacter.fromJson(Map<String, dynamic> json) => AiCharacter(
        id: json['id'] as String,
        name: json['name'] as String,
        role: json['role'] as String? ?? 'AI 角色',
        description: json['description'] as String? ?? '',
        firstMessage: json['firstMessage'] as String? ?? '你好，我在。',
      );
}

class ChatMessage {
  ChatMessage({
    required this.id,
    required this.characterId,
    required this.content,
    required this.isUser,
    required this.time,
  });

  final String id;
  final String characterId;
  final String content;
  final bool isUser;
  final String time;

  Map<String, dynamic> toJson() => {
        'id': id,
        'characterId': characterId,
        'content': content,
        'isUser': isUser,
        'time': time,
      };

  factory ChatMessage.fromJson(Map<String, dynamic> json) => ChatMessage(
        id: json['id'] as String,
        characterId: json['characterId'] as String,
        content: json['content'] as String,
        isUser: json['isUser'] as bool,
        time: json['time'] as String? ?? '刚刚',
      );
}

class ApiConfig {
  ApiConfig({
    this.baseUrl = 'https://api.openai.com',
    this.apiKey = '',
    this.model = 'gpt-4o-mini',
  });

  String baseUrl;
  String apiKey;
  String model;

  Map<String, dynamic> toJson() => {'baseUrl': baseUrl, 'apiKey': apiKey, 'model': model};

  factory ApiConfig.fromJson(Map<String, dynamic> json) => ApiConfig(
        baseUrl: json['baseUrl'] as String? ?? 'https://api.openai.com',
        apiKey: json['apiKey'] as String? ?? '',
        model: json['model'] as String? ?? 'gpt-4o-mini',
      );
}

class AiChatApp extends StatelessWidget {
  const AiChatApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: '酒馆 AI 聊天',
      theme: ThemeData(
        useMaterial3: true,
        scaffoldBackgroundColor: bg,
        colorScheme: ColorScheme.fromSeed(seedColor: blue, primary: blue, surface: bg),
        fontFamily: 'sans',
      ),
      home: const HomeScreen(),
    );
  }
}

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  static const storeKey = 'aichat_flutter_state';
  int tab = 0;
  String screen = 'main';
  String? selectedCharacterId = 'star';
  bool sending = false;
  bool checking = false;
  bool loadingModels = false;
  String connectionStatus = '尚未验证连接';
  String profileName = '旅行者';
  String profileTitle = 'AI 聊天玩家';
  String profileAvatarBase64 = '';
  List<Map<String, String>> profileFields = [
    {'key': '地区', 'value': '未设置'},
    {'key': '偏好', 'value': '角色扮演'},
  ];
  List<String> models = [];
  final messageController = TextEditingController();
  final searchController = TextEditingController();
  final api = ApiConfig();
  late final TextEditingController baseUrlController;
  late final TextEditingController keyController;
  late final TextEditingController modelController;

  List<AiCharacter> characters = [
    AiCharacter(
      id: 'star',
      name: '星河',
      role: '温柔向导',
      description: '擅长陪伴、梳理思绪和进行轻剧情对话的 AI 角色。',
      firstMessage: '欢迎回来。今天想让我陪你聊天，还是一起整理一个故事？',
    ),
    AiCharacter(
      id: 'lin',
      name: '林澜',
      role: '故事搭档',
      description: '适合长篇角色扮演，会维护世界观和人物关系。',
      firstMessage: '你终于来了。我刚找到一页很奇怪的手稿。',
    ),
    AiCharacter(
      id: 'ink',
      name: '墨蓝',
      role: '设定顾问',
      description: '帮助创建角色卡、润色开场白、补全世界设定。',
      firstMessage: '把你的角色草稿给我，我会先帮你抓住核心冲突。',
    ),
  ];

  List<ChatMessage> messages = [
    ChatMessage(
      id: 'm1',
      characterId: 'star',
      content: '欢迎回来。今天想让我陪你聊天，还是一起整理一个故事？',
      isUser: false,
      time: '10:42',
    ),
  ];

  @override
  void initState() {
    super.initState();
    baseUrlController = TextEditingController(text: api.baseUrl);
    keyController = TextEditingController(text: api.apiKey);
    modelController = TextEditingController(text: api.model);
    loadState();
  }

  @override
  void dispose() {
    messageController.dispose();
    searchController.dispose();
    baseUrlController.dispose();
    keyController.dispose();
    modelController.dispose();
    super.dispose();
  }

  AiCharacter get selectedCharacter => characters.firstWhere(
        (item) => item.id == selectedCharacterId,
        orElse: () => characters.first,
      );

  List<ChatMessage> get activeMessages =>
      messages.where((message) => message.characterId == selectedCharacter.id).toList();

  Future<void> loadState() async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString(storeKey);
    if (raw == null) return;
    try {
      final json = jsonDecode(raw) as Map<String, dynamic>;
      setState(() {
        characters = (json['characters'] as List).cast<Map<String, dynamic>>().map(AiCharacter.fromJson).toList();
        messages = (json['messages'] as List).cast<Map<String, dynamic>>().map(ChatMessage.fromJson).toList();
        final savedApi = ApiConfig.fromJson(json['api'] as Map<String, dynamic>);
        api.baseUrl = savedApi.baseUrl;
        api.apiKey = savedApi.apiKey;
        api.model = savedApi.model;
        final profile = json['profile'] is Map<String, dynamic> ? json['profile'] as Map<String, dynamic> : <String, dynamic>{};
        profileName = profile['name'] as String? ?? profileName;
        profileTitle = profile['title'] as String? ?? profileTitle;
        profileAvatarBase64 = profile['avatar'] as String? ?? profileAvatarBase64;
        profileFields = (profile['fields'] as List?)
                ?.whereType<Map>()
                .map((item) => {
                      'key': item['key']?.toString() ?? '',
                      'value': item['value']?.toString() ?? '',
                    })
                .where((item) => item['key']!.trim().isNotEmpty || item['value']!.trim().isNotEmpty)
                .toList() ??
            profileFields;
        selectedCharacterId = characters.first.id;
        baseUrlController.text = api.baseUrl;
        keyController.text = api.apiKey;
        modelController.text = api.model;
      });
    } catch (_) {}
  }

  Future<void> persist() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(
      storeKey,
      jsonEncode({
        'characters': characters.map((item) => item.toJson()).toList(),
        'messages': messages.map((item) => item.toJson()).toList(),
        'api': api.toJson(),
        'profile': {
          'name': profileName,
          'title': profileTitle,
          'avatar': profileAvatarBase64,
          'fields': profileFields,
        },
      }),
    );
  }

  String normalizeBaseUrl(String value) => value.trim().replaceFirst(RegExp(r'/+$'), '');

  Map<String, String> headers() => {
        'Accept': 'application/json',
        if (api.apiKey.trim().isNotEmpty) 'Authorization': 'Bearer ${api.apiKey.trim()}',
      };

  Future<List<String>> fetchModels() async {
    saveApi(silent: true);
    if (api.baseUrl.trim().isEmpty) throw Exception('请先填写 Base URL');
    if (api.apiKey.trim().isEmpty) throw Exception('请先填写 API Key');
    final response = await http.get(Uri.parse('${normalizeBaseUrl(api.baseUrl)}/v1/models'), headers: headers());
    final raw = response.body;
    final data = raw.isEmpty ? <String, dynamic>{} : jsonDecode(raw) as Map<String, dynamic>;
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw Exception(data['error']?['message'] ?? data['message'] ?? 'HTTP ${response.statusCode}');
    }
    final list = data['data'] is List ? data['data'] as List : data['models'] is List ? data['models'] as List : [];
    return list
        .map((item) => item is String ? item : item is Map ? (item['id'] ?? item['name'])?.toString() : null)
        .whereType<String>()
        .toList()
      ..sort();
  }

  Future<void> verifyConnection() async {
    setState(() {
      checking = true;
      connectionStatus = '正在验证连接...';
    });
    try {
      final list = await fetchModels();
      setState(() {
        models = list;
        connectionStatus = list.isEmpty ? '连接成功，但没有返回模型' : '连接成功，发现 ${list.length} 个模型';
      });
    } catch (error) {
      setState(() => connectionStatus = '连接失败：${friendlyError(error)}');
    } finally {
      setState(() => checking = false);
    }
  }

  Future<void> loadModels() async {
    setState(() => loadingModels = true);
    try {
      final list = await fetchModels();
      setState(() {
        models = list;
        connectionStatus = list.isEmpty ? '接口可用，但没有返回模型' : '已获取 ${list.length} 个模型';
      });
    } catch (error) {
      setState(() => connectionStatus = '获取失败：${friendlyError(error)}');
    } finally {
      setState(() => loadingModels = false);
    }
  }

  String friendlyError(Object error) {
    final text = error.toString().replaceFirst('Exception: ', '');
    if (text.contains('Connection') || text.contains('SocketException')) {
      return '网络连接失败，请检查地址、Key 或网络';
    }
    return text;
  }

  void saveApi({bool silent = false}) {
    api.baseUrl = normalizeBaseUrl(baseUrlController.text);
    api.apiKey = keyController.text.trim();
    api.model = modelController.text.trim().isEmpty ? 'gpt-4o-mini' : modelController.text.trim();
    persist();
    if (!silent) setState(() => connectionStatus = '配置已保存，建议验证连接');
  }

  Future<void> sendMessage() async {
    final text = messageController.text.trim();
    if (text.isEmpty || sending) return;
    messageController.clear();
    final character = selectedCharacter;
    setState(() {
      messages.add(ChatMessage(
        id: DateTime.now().microsecondsSinceEpoch.toString(),
        characterId: character.id,
        content: text,
        isUser: true,
        time: '刚刚',
      ));
      sending = true;
    });
    await persist();
    if (api.apiKey.trim().isEmpty) {
      appendAssistant(character.id, '请先在“我的 > API 管理”中填写并验证 API。');
      return;
    }
    try {
      final reply = await requestReply(character);
      appendAssistant(character.id, reply.isEmpty ? '模型没有返回内容。' : reply);
    } catch (error) {
      appendAssistant(character.id, '请求失败：${friendlyError(error)}');
    }
  }

  Future<String> requestReply(AiCharacter character) async {
    final system = [
      '你是一个 AI 聊天角色。请全程使用中文自然回复。',
      '角色名：${character.name}',
      '定位：${character.role}',
      '简介：${character.description}',
      '开场风格：${character.firstMessage}',
    ].join('\n');
    final chatMessages = [
      {'role': 'system', 'content': system},
      ...messages
          .where((message) => message.characterId == character.id)
          .toList()
          .reversed
          .take(20)
          .toList()
          .reversed
          .map((message) => {
                'role': message.isUser ? 'user' : 'assistant',
                'content': message.content,
              }),
    ];
    final response = await http.post(
      Uri.parse('${normalizeBaseUrl(api.baseUrl)}/v1/chat/completions'),
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ${api.apiKey.trim()}',
      },
      body: jsonEncode({'model': api.model, 'messages': chatMessages, 'temperature': 0.8}),
    );
    final data = response.body.isEmpty ? <String, dynamic>{} : jsonDecode(response.body) as Map<String, dynamic>;
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw Exception(data['error']?['message'] ?? data['message'] ?? 'HTTP ${response.statusCode}');
    }
    return data['choices']?[0]?['message']?['content']?.toString().trim() ?? '';
  }

  void appendAssistant(String characterId, String content) {
    setState(() {
      messages.add(ChatMessage(
        id: DateTime.now().microsecondsSinceEpoch.toString(),
        characterId: characterId,
        content: content,
        isUser: false,
        time: '刚刚',
      ));
      sending = false;
    });
    persist();
  }

  void ensureGreeting(AiCharacter character) {
    final firstMessage = character.firstMessage.trim();
    if (firstMessage.isEmpty) return;
    final hasMessages = messages.any((message) => message.characterId == character.id);
    if (hasMessages) return;
    messages.add(ChatMessage(
      id: DateTime.now().microsecondsSinceEpoch.toString(),
      characterId: character.id,
      content: firstMessage,
      isUser: false,
      time: '刚刚',
    ));
    persist();
  }

  void createCharacter() {
    setState(() {
      characters.insert(
        0,
        AiCharacter(
          id: DateTime.now().microsecondsSinceEpoch.toString(),
          name: '新角色',
          role: '自定义',
          description: '可以后续接入 JSON / PNG 角色卡导入。',
          firstMessage: '你好，我是新角色。',
        ),
      );
    });
    persist();
  }

  Future<void> editCharacter(AiCharacter character) async {
    final nameController = TextEditingController(text: character.name);
    final roleController = TextEditingController(text: character.role);
    final descriptionController = TextEditingController(text: character.description);
    final firstMessageController = TextEditingController(text: character.firstMessage);
    final updated = await showDialog<AiCharacter>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('编辑角色'),
        content: SingleChildScrollView(
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            TextField(controller: nameController, decoration: inputDecoration('角色名称')),
            const SizedBox(height: 12),
            TextField(controller: roleController, decoration: inputDecoration('角色定位')),
            const SizedBox(height: 12),
            TextField(controller: descriptionController, minLines: 3, maxLines: 7, decoration: inputDecoration('角色描述')),
            const SizedBox(height: 12),
            TextField(controller: firstMessageController, minLines: 2, maxLines: 5, decoration: inputDecoration('开场白')),
          ]),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('取消')),
          FilledButton(
            onPressed: () {
              final name = nameController.text.trim();
              if (name.isEmpty) return;
              Navigator.pop(
                context,
                character.copyWith(
                  name: name,
                  role: roleController.text.trim().isEmpty ? '自定义角色' : roleController.text.trim(),
                  description: descriptionController.text.trim(),
                  firstMessage: firstMessageController.text.trim().isEmpty ? '你好，我在。' : firstMessageController.text.trim(),
                ),
              );
            },
            child: const Text('保存'),
          ),
        ],
      ),
    );
    if (updated == null) return;
    setState(() {
      final index = characters.indexWhere((item) => item.id == character.id);
      if (index >= 0) characters[index] = updated;
    });
    await persist();
  }

  Future<void> importCharacter() async {
    try {
      final result = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: ['json', 'png'],
        withData: true,
      );
      final file = result?.files.single;
      final bytes = file?.bytes;
      if (file == null || bytes == null) return;
      final character = file.extension?.toLowerCase() == 'png'
          ? parsePngCharacter(bytes)
          : characterFromJson(jsonDecode(utf8.decode(bytes)) as Map<String, dynamic>);
      setState(() {
        characters.insert(0, character);
        selectedCharacterId = character.id;
        ensureGreeting(character);
      });
      await persist();
      showSnack('已导入 ${character.name}');
    } catch (error) {
      showSnack('导入失败：${friendlyError(error)}');
    }
  }

  AiCharacter parsePngCharacter(List<int> bytes) {
    if (bytes.length < 12 || bytes[0] != 0x89 || bytes[1] != 0x50 || bytes[2] != 0x4E || bytes[3] != 0x47) {
      throw Exception('不是有效的 PNG 角色卡');
    }
    var offset = 8;
    while (offset + 12 <= bytes.length) {
      final length = readUint32(bytes, offset);
      final type = ascii.decode(bytes.sublist(offset + 4, offset + 8));
      final dataStart = offset + 8;
      final dataEnd = dataStart + length;
      if (dataEnd > bytes.length) break;
      if (type == 'tEXt') {
        final chunk = bytes.sublist(dataStart, dataEnd);
        final separator = chunk.indexOf(0);
        if (separator > 0) {
          final key = latin1.decode(chunk.sublist(0, separator));
          final value = latin1.decode(chunk.sublist(separator + 1));
          if (key == 'chara') {
            final decoded = utf8.decode(base64.decode(value));
            return characterFromJson(jsonDecode(decoded) as Map<String, dynamic>);
          }
        }
      }
      offset = dataEnd + 4;
    }
    throw Exception('没有找到 SillyTavern 角色数据');
  }

  int readUint32(List<int> bytes, int offset) =>
      (bytes[offset] << 24) | (bytes[offset + 1] << 16) | (bytes[offset + 2] << 8) | bytes[offset + 3];

  AiCharacter characterFromJson(Map<String, dynamic> raw) {
    final data = raw['data'] is Map<String, dynamic> ? raw['data'] as Map<String, dynamic> : raw;
    final name = pickText(data, ['name', 'char_name'], fallback: pickText(raw, ['name', 'char_name'], fallback: '导入角色'));
    final personality = pickText(data, ['personality']);
    final scenario = pickText(data, ['scenario']);
    final creatorNotes = pickText(data, ['creator_notes', 'creatorNotes']);
    final description = [
      pickText(data, ['description', 'desc']),
      if (personality.isNotEmpty) '性格：$personality',
      if (scenario.isNotEmpty) '场景：$scenario',
      if (creatorNotes.isNotEmpty) '备注：$creatorNotes',
    ].where((item) => item.trim().isNotEmpty).join('\n\n');
    return AiCharacter(
      id: DateTime.now().microsecondsSinceEpoch.toString(),
      name: name,
      role: pickText(data, ['role', 'tags'], fallback: personality.isEmpty ? '导入角色' : personality).split('\n').first,
      description: description.isEmpty ? '从角色卡导入。' : description,
      firstMessage: pickText(data, ['first_mes', 'firstMessage', 'first_message', 'mes_example'], fallback: '你好，我在。'),
    );
  }

  String pickText(Map<String, dynamic> json, List<String> keys, {String fallback = ''}) {
    for (final key in keys) {
      final value = json[key];
      if (value is String && value.trim().isNotEmpty) return value.trim();
      if (value is List && value.isNotEmpty) return value.map((item) => item.toString()).join(', ');
    }
    return fallback;
  }

  void showSnack(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }

  Future<void> changeProfileAvatar() async {
    try {
      final result = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: ['png', 'jpg', 'jpeg', 'webp'],
        withData: true,
      );
      final bytes = result?.files.single.bytes;
      if (bytes == null) return;
      setState(() => profileAvatarBase64 = base64Encode(bytes));
      await persist();
      showSnack('头像已更新');
    } catch (error) {
      showSnack('头像更新失败：${friendlyError(error)}');
    }
  }

  Future<void> editProfile() async {
    final nameController = TextEditingController(text: profileName);
    final titleController = TextEditingController(text: profileTitle);
    final updated = await showDialog<List<String>>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('我的资料'),
        content: Column(mainAxisSize: MainAxisSize.min, children: [
          TextField(controller: nameController, decoration: inputDecoration('昵称')),
          const SizedBox(height: 12),
          TextField(controller: titleController, minLines: 2, maxLines: 4, decoration: inputDecoration('身份 / 签名')),
        ]),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('取消')),
          FilledButton(
            onPressed: () {
              final name = nameController.text.trim();
              if (name.isEmpty) return;
              Navigator.pop(context, [name, titleController.text.trim().isEmpty ? 'AI 聊天玩家' : titleController.text.trim()]);
            },
            child: const Text('保存'),
          ),
        ],
      ),
    );
    if (updated == null) return;
    setState(() {
      profileName = updated[0];
      profileTitle = updated[1];
    });
    await persist();
    showSnack('资料已保存');
  }

  @override
  Widget build(BuildContext context) {
    final isMainScreen = screen == 'main';
    return PopScope(
      canPop: isMainScreen,
      onPopInvokedWithResult: (didPop, result) {
        if (!didPop && !isMainScreen) {
          setState(() => screen = 'main');
        }
      },
      child: screen == 'chat'
          ? chatScreen()
          : screen == 'api'
              ? apiScreen()
              : screen == 'profile'
                  ? profileScreen()
                  : mainScreen(),
    );
  }

  Widget mainScreen() => Scaffold(
      appBar: AppBar(
        title: Text(tab == 0 ? '聊天' : tab == 1 ? '角色' : '酒馆 AI'),
        backgroundColor: bg.withValues(alpha: 0.94),
        actions: [
          if (tab == 1) IconButton(onPressed: importCharacter, icon: const Icon(Icons.upload_file_outlined)),
          if (tab == 1) IconButton(onPressed: createCharacter, icon: const Icon(Icons.add_circle_outline)),
        ],
      ),
      body: IndexedStack(index: tab, children: [chatsPage(), charactersPage(), mePage()]),
      bottomNavigationBar: NavigationBar(
        selectedIndex: tab,
        onDestinationSelected: (value) => setState(() => tab = value),
        backgroundColor: Colors.white,
        destinations: const [
          NavigationDestination(icon: Icon(Icons.chat_bubble_outline), selectedIcon: Icon(Icons.chat_bubble), label: '聊天'),
          NavigationDestination(icon: Icon(Icons.smart_toy_outlined), selectedIcon: Icon(Icons.smart_toy), label: '角色'),
          NavigationDestination(icon: Icon(Icons.person_outline), selectedIcon: Icon(Icons.person), label: '我的'),
        ],
      ),
    );

  Widget pagePadding(Widget child) => SafeArea(
        top: false,
        child: ListView(padding: const EdgeInsets.fromLTRB(18, 18, 18, 26), children: [child]),
      );

  Widget chatsPage() => pagePadding(Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          hero('AI Chat Hub', '把角色、聊天和模型连接放在一个安静清晰的空间里。', Icons.auto_awesome),
          const SizedBox(height: 24),
          sectionTitle('最近消息'),
          ...characters.map((character) {
            final last = messages.where((message) => message.characterId == character.id).lastOrNull;
            return listCard(
              onTap: () {
                ensureGreeting(character);
                setState(() {
                  selectedCharacterId = character.id;
                  screen = 'chat';
                });
              },
              child: Row(children: [
                avatar(character.name),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    Text(character.name, style: titleStyle()),
                    const SizedBox(height: 4),
                    Text(last?.content ?? character.firstMessage, maxLines: 1, overflow: TextOverflow.ellipsis, style: mutedStyle()),
                  ]),
                ),
                const Text('刚刚', style: TextStyle(color: textMuted, fontSize: 12)),
              ]),
            );
          }),
        ],
      ));

  Widget charactersPage() {
    final keyword = searchController.text.trim();
    final filtered = keyword.isEmpty
        ? characters
        : characters
            .where((item) => '${item.name}${item.role}${item.description}'.toLowerCase().contains(keyword.toLowerCase()))
            .toList();
    return ListView(
      padding: const EdgeInsets.fromLTRB(18, 18, 18, 26),
      children: [
        TextField(
          controller: searchController,
          onChanged: (_) => setState(() {}),
          decoration: inputDecoration('搜索角色...', icon: Icons.search),
        ),
        const SizedBox(height: 16),
        ...filtered.map((character) => listCard(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Row(children: [
                  avatar(character.name),
                  const SizedBox(width: 14),
                  Expanded(child: Text(character.name, style: titleStyle())),
                  IconButton(
                    tooltip: '编辑角色',
                    onPressed: () => editCharacter(character),
                    icon: const Icon(Icons.edit_outlined, color: blue),
                  ),
                  TextButton(
                    onPressed: () => setState(() {
                      characters.removeWhere((item) => item.id == character.id);
                      messages.removeWhere((item) => item.characterId == character.id);
                      persist();
                    }),
                    child: const Text('删除', style: TextStyle(color: danger)),
                  ),
                ]),
                const SizedBox(height: 10),
                Chip(label: Text(character.role), backgroundColor: lightBlue),
                const SizedBox(height: 8),
                Text(character.description, style: mutedStyle()),
              ]),
              onTap: () {
                ensureGreeting(character);
                setState(() {
                  selectedCharacterId = character.id;
                  screen = 'chat';
                });
              },
            )),
        OutlinedButton.icon(
          onPressed: createCharacter,
          icon: const Icon(Icons.add),
          label: const Text('创建角色'),
          style: OutlinedButton.styleFrom(minimumSize: const Size.fromHeight(60), shape: rounded(18)),
        ),
        const SizedBox(height: 12),
        FilledButton.icon(
          onPressed: importCharacter,
          icon: const Icon(Icons.upload_file),
          label: const Text('导入 JSON / 酒馆角色卡'),
          style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(60), shape: rounded(18)),
        ),
      ],
    );
  }

  Widget mePage() => pagePadding(Column(children: [
        const SizedBox(height: 12),
        Stack(
          children: [
            avatar(profileName, size: 116, imageBase64: profileAvatarBase64, onTap: changeProfileAvatar),
            Positioned(
              right: 2,
              bottom: 2,
              child: Material(
                color: blue,
                shape: const CircleBorder(),
                child: InkWell(
                  customBorder: const CircleBorder(),
                  onTap: changeProfileAvatar,
                  child: const Padding(
                    padding: EdgeInsets.all(9),
                    child: Icon(Icons.photo_camera_outlined, color: Colors.white, size: 20),
                  ),
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 14),
        Text(profileName, style: titleStyle(size: 24)),
        const SizedBox(height: 4),
        Text(profileTitle, textAlign: TextAlign.center, style: const TextStyle(color: textMuted)),
        const SizedBox(height: 28),
        menuCard(Icons.wifi, 'API 管理', '验证连接、获取模型列表、保存默认模型', () => setState(() => screen = 'api')),
        menuCard(Icons.person_outline, '我的资料', '昵称、身份和偏好设置', () => setState(() => screen = 'profile')),
        tipCard('本地优先', '当前 Flutter 版会把 API 配置、资料和聊天保存到本机。正式发布前建议改成加密存储。'),
      ]));

  Widget profileScreen() => Scaffold(
        appBar: AppBar(
          leading: IconButton(onPressed: () => setState(() => screen = 'main'), icon: const Icon(Icons.arrow_back)),
          title: const Text('编辑资料'),
          backgroundColor: Colors.white,
        ),
        body: ListView(
          padding: const EdgeInsets.fromLTRB(18, 22, 18, 110),
          children: [
            Center(
              child: Column(children: [
                Stack(children: [
                  avatar(profileName, size: 128, imageBase64: profileAvatarBase64, onTap: changeProfileAvatar),
                  Positioned(
                    right: 2,
                    bottom: 2,
                    child: Material(
                      color: blue,
                      shape: const CircleBorder(),
                      child: InkWell(
                        customBorder: const CircleBorder(),
                        onTap: changeProfileAvatar,
                        child: const Padding(
                          padding: EdgeInsets.all(10),
                          child: Icon(Icons.edit_outlined, color: Colors.white, size: 20),
                        ),
                      ),
                    ),
                  ),
                ]),
                const SizedBox(height: 14),
                Text(profileName, style: titleStyle(size: 22)),
                const SizedBox(height: 4),
                Text(profileTitle, style: const TextStyle(color: textMuted, fontSize: 13, fontWeight: FontWeight.w700)),
              ]),
            ),
            const SizedBox(height: 28),
            profileInput(
              label: '昵称',
              value: profileName,
              hint: '别人看到你的名字',
              onChanged: (value) => profileName = value.trim().isEmpty ? '旅行者' : value.trim(),
            ),
            const SizedBox(height: 16),
            profileInput(
              label: '身份 / 签名',
              value: profileTitle,
              hint: '一句话描述你的身份、偏好或当前状态',
              maxLines: 2,
              onChanged: (value) => profileTitle = value.trim().isEmpty ? 'AI 聊天玩家' : value.trim(),
            ),
            const SizedBox(height: 26),
            Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
              Text('自定义字段', style: titleStyle(size: 18)),
              TextButton.icon(
                onPressed: () => setState(() => profileFields.add({'key': '新字段', 'value': ''})),
                icon: const Icon(Icons.add),
                label: const Text('新增字段'),
              ),
            ]),
            const SizedBox(height: 10),
            ...profileFields.asMap().entries.map((entry) => customProfileField(entry.key, entry.value)),
          ],
        ),
        bottomNavigationBar: SafeArea(
          child: Container(
            padding: const EdgeInsets.fromLTRB(18, 12, 18, 18),
            color: Colors.white.withValues(alpha: 0.94),
            child: FilledButton.icon(
              onPressed: () async {
                setState(() {
                  profileFields = profileFields
                      .where((item) => item['key']!.trim().isNotEmpty || item['value']!.trim().isNotEmpty)
                      .toList();
                });
                await persist();
                showSnack('资料已保存');
              },
              icon: const Icon(Icons.check),
              label: const Text('保存资料'),
              style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(56), shape: rounded(16)),
            ),
          ),
        ),
      );

  Widget profileInput({
    required String label,
    required String value,
    required String hint,
    required ValueChanged<String> onChanged,
    int maxLines = 1,
  }) =>
      Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Padding(
          padding: const EdgeInsets.only(left: 4, bottom: 7),
          child: Text(label, style: const TextStyle(color: textMuted, fontWeight: FontWeight.w800, fontSize: 13)),
        ),
        TextFormField(
          initialValue: value,
          maxLines: maxLines,
          onChanged: onChanged,
          decoration: inputDecoration(hint),
        ),
      ]);

  Widget customProfileField(int index, Map<String, String> field) => Card(
        color: Colors.white,
        elevation: 0,
        margin: const EdgeInsets.only(bottom: 12),
        shape: rounded(16),
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Row(children: [
            Expanded(
              child: Column(children: [
                TextFormField(
                  key: ValueKey('profile-key-$index-${field['key']}'),
                  initialValue: field['key'],
                  onChanged: (value) => profileFields[index]['key'] = value,
                  decoration: inputDecoration('字段名', helper: '比如地区、职业、偏好、世界观标签。'),
                ),
                const SizedBox(height: 10),
                TextFormField(
                  key: ValueKey('profile-value-$index-${field['value']}'),
                  initialValue: field['value'],
                  onChanged: (value) => profileFields[index]['value'] = value,
                  decoration: inputDecoration('字段内容', helper: '这个字段对应的具体内容。'),
                ),
              ]),
            ),
            IconButton(
              tooltip: '删除字段',
              onPressed: () => setState(() => profileFields.removeAt(index)),
              icon: const Icon(Icons.delete_outline, color: danger),
            ),
          ]),
        ),
      );

  Widget chatScreen() => Scaffold(
        appBar: AppBar(
          leading: IconButton(onPressed: () => setState(() => screen = 'main'), icon: const Icon(Icons.arrow_back)),
          title: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(selectedCharacter.name, style: titleStyle(size: 18)),
            const Text('在线', style: TextStyle(color: textMuted, fontSize: 12)),
          ]),
          backgroundColor: bg.withValues(alpha: 0.94),
        ),
        body: Column(children: [
          Expanded(
            child: ListView(
              padding: const EdgeInsets.all(18),
              children: [
                Center(child: Chip(label: const Text('今天'), backgroundColor: lightBlue)),
                const SizedBox(height: 10),
                ...activeMessages.map(messageBubble),
                if (sending) messageBubble(ChatMessage(id: 'typing', characterId: selectedCharacter.id, content: '${selectedCharacter.name} 正在思考...', isUser: false, time: '')),
              ],
            ),
          ),
          SafeArea(
            child: Container(
              padding: const EdgeInsets.all(12),
              color: Colors.white,
              child: Row(children: [
                Expanded(
                  child: TextField(
                    controller: messageController,
                    enabled: !sending,
                    minLines: 1,
                    maxLines: 4,
                    decoration: inputDecoration('输入消息...'),
                  ),
                ),
                const SizedBox(width: 10),
                FilledButton(
                  onPressed: sending ? null : sendMessage,
                  style: FilledButton.styleFrom(shape: rounded(16), minimumSize: const Size(54, 54), padding: EdgeInsets.zero),
                  child: const Icon(Icons.send_rounded),
                ),
              ]),
            ),
          ),
        ]),
      );

  Widget apiScreen() => Scaffold(
        appBar: AppBar(
          leading: IconButton(onPressed: () => setState(() => screen = 'main'), icon: const Icon(Icons.arrow_back)),
          title: const Text('API 管理'),
          actions: [IconButton(onPressed: checking ? null : verifyConnection, icon: const Icon(Icons.refresh))],
          backgroundColor: bg.withValues(alpha: 0.94),
        ),
        body: ListView(
          padding: const EdgeInsets.fromLTRB(18, 18, 18, 28),
          children: [
            hero('Manage Connectivity', '配置 OpenAI 兼容接口，验证连接并拉取可用模型。', Icons.key_outlined,
                action: FilledButton.icon(onPressed: checking ? null : verifyConnection, icon: const Icon(Icons.wifi), label: Text(checking ? '验证中...' : '验证连接'))),
            const SizedBox(height: 18),
            statusCard(),
            const SizedBox(height: 18),
            formCard(),
            const SizedBox(height: 18),
            modelPanel(),
            const SizedBox(height: 18),
            tipCard('安全提示', '安装版当前使用 SharedPreferences 保存 Key。后续可以换成 Android Keystore / iOS Keychain。'),
          ],
        ),
      );

  Widget formCard() => Card(
        color: Colors.white,
        elevation: 0,
        shape: rounded(20),
        child: Padding(
          padding: const EdgeInsets.all(18),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            sectionTitle('服务配置'),
            TextField(controller: baseUrlController, decoration: inputDecoration('Base URL')),
            const SizedBox(height: 12),
            TextField(controller: keyController, obscureText: true, decoration: inputDecoration('API Key')),
            const SizedBox(height: 12),
            TextField(controller: modelController, decoration: inputDecoration('默认模型')),
            const SizedBox(height: 14),
            Row(children: [
              Expanded(child: OutlinedButton.icon(onPressed: saveApi, icon: const Icon(Icons.shield_outlined), label: const Text('保存配置'))),
              const SizedBox(width: 10),
              Expanded(child: FilledButton.icon(onPressed: loadingModels ? null : loadModels, icon: const Icon(Icons.list_alt), label: Text(loadingModels ? '获取中' : '获取模型'))),
            ]),
          ]),
        ),
      );

  Widget modelPanel() => Card(
        color: Colors.white,
        elevation: 0,
        shape: rounded(20),
        child: Padding(
          padding: const EdgeInsets.all(18),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
              Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                sectionTitle('可用模型'),
                Text(models.isEmpty ? '获取后可点选默认模型' : '已获取 ${models.length} 个模型', style: mutedStyle()),
              ]),
              IconButton(onPressed: loadingModels ? null : loadModels, icon: const Icon(Icons.refresh, color: blue)),
            ]),
            const SizedBox(height: 8),
            if (models.isEmpty)
              OutlinedButton.icon(onPressed: loadingModels ? null : loadModels, icon: const Icon(Icons.science_outlined), label: const Text('获取可用的大模型列表'))
            else
              ...models.map((model) => Padding(
                    padding: const EdgeInsets.only(bottom: 8),
                    child: ListTile(
                      shape: rounded(14),
                      tileColor: model == api.model ? lightBlue : const Color(0xFFEFF4FF),
                      leading: const Icon(Icons.smart_toy_outlined, color: blue),
                      title: Text(model, maxLines: 1, overflow: TextOverflow.ellipsis),
                      trailing: model == api.model ? const Icon(Icons.check_circle, color: blue) : null,
                      onTap: () {
                        modelController.text = model;
                        saveApi();
                      },
                    ),
                  )),
          ]),
        ),
      );

  Widget statusCard() => Card(
        color: Colors.white,
        elevation: 0,
        shape: rounded(20),
        child: ListTile(
          leading: CircleAvatar(
            backgroundColor: connectionStatus.contains('失败') ? const Color(0xFFFFDAD6) : lightBlue,
            child: Icon(connectionStatus.contains('失败') ? Icons.wifi_off : Icons.wifi, color: connectionStatus.contains('失败') ? danger : blue),
          ),
          title: Text(connectionStatus.contains('失败') ? '连接异常' : '连接状态', style: titleStyle(size: 16)),
          subtitle: Text(connectionStatus),
        ),
      );

  Widget messageBubble(ChatMessage message) => Align(
        alignment: message.isUser ? Alignment.centerRight : Alignment.centerLeft,
        child: Column(
          crossAxisAlignment: message.isUser ? CrossAxisAlignment.end : CrossAxisAlignment.start,
          children: [
            Container(
              constraints: const BoxConstraints(maxWidth: 310),
              margin: const EdgeInsets.only(bottom: 4, top: 8),
              padding: const EdgeInsets.symmetric(horizontal: 15, vertical: 12),
              decoration: BoxDecoration(
                color: message.isUser ? blue : Colors.white,
                borderRadius: BorderRadius.only(
                  topLeft: const Radius.circular(18),
                  topRight: const Radius.circular(18),
                  bottomLeft: Radius.circular(message.isUser ? 18 : 5),
                  bottomRight: Radius.circular(message.isUser ? 5 : 18),
                ),
              ),
              child: Text(message.content, style: TextStyle(color: message.isUser ? Colors.white : textMain, height: 1.45)),
            ),
            if (message.time.isNotEmpty) Text(message.time, style: const TextStyle(color: textMuted, fontSize: 11)),
          ],
        ),
      );

  Widget hero(String title, String subtitle, IconData icon, {Widget? action}) => Container(
        width: double.infinity,
        padding: const EdgeInsets.all(26),
        decoration: BoxDecoration(
          color: blue,
          borderRadius: BorderRadius.circular(22),
          boxShadow: const [BoxShadow(color: Color(0x22002A5C), blurRadius: 24, offset: Offset(0, 12))],
        ),
        child: Row(children: [
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(title, style: const TextStyle(color: Colors.white, fontSize: 29, height: 1.1, fontWeight: FontWeight.w800)),
              const SizedBox(height: 8),
              Text(subtitle, style: const TextStyle(color: Colors.white, fontSize: 16, height: 1.42)),
              if (action != null) Padding(padding: const EdgeInsets.only(top: 18), child: action),
            ]),
          ),
          Icon(icon, color: Colors.white.withValues(alpha: 0.24), size: 78),
        ]),
      );

  Widget listCard({required Widget child, VoidCallback? onTap}) => Card(
        color: Colors.white,
        elevation: 0,
        margin: const EdgeInsets.only(bottom: 14),
        shape: rounded(20),
        child: InkWell(borderRadius: BorderRadius.circular(20), onTap: onTap, child: Padding(padding: const EdgeInsets.all(16), child: child)),
      );

  Widget menuCard(IconData icon, String title, String subtitle, VoidCallback onTap) => listCard(
        onTap: onTap,
        child: Row(children: [
          CircleAvatar(backgroundColor: lightBlue, child: Icon(icon, color: blue)),
          const SizedBox(width: 14),
          Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(title, style: titleStyle()), const SizedBox(height: 4), Text(subtitle, style: mutedStyle())])),
          const Icon(Icons.chevron_right, color: textMuted),
        ]),
      );

  Widget tipCard(String title, String subtitle) => Card(
        color: const Color(0xFFEFF5FF),
        elevation: 0,
        shape: rounded(20),
        child: Padding(
          padding: const EdgeInsets.all(18),
          child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
            const Icon(Icons.auto_awesome, color: blue),
            const SizedBox(width: 14),
            Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(title, style: titleStyle(size: 16)), const SizedBox(height: 6), Text(subtitle, style: mutedStyle())])),
          ]),
        ),
      );

  Widget avatar(String name, {double size = 54, String? imageBase64, VoidCallback? onTap}) {
    ImageProvider? image;
    if (imageBase64 != null && imageBase64.isNotEmpty) {
      try {
        image = MemoryImage(base64Decode(imageBase64));
      } catch (_) {}
    }
    final initials = name.trim().isEmpty ? '?' : name.trim().substring(0, name.trim().length < 2 ? 1 : 2);
    final circle = CircleAvatar(
      radius: size / 2,
      backgroundColor: lightBlue,
      backgroundImage: image,
      child: image == null ? Text(initials, style: TextStyle(color: blue, fontWeight: FontWeight.w800, fontSize: size > 80 ? 28 : 16)) : null,
    );
    if (onTap == null) return circle;
    return InkWell(borderRadius: BorderRadius.circular(size / 2), onTap: onTap, child: circle);
  }

  Widget sectionTitle(String value) => Text(value, style: const TextStyle(color: Color(0xFF858B99), fontSize: 12, fontWeight: FontWeight.w800, letterSpacing: 1.4));

  TextStyle titleStyle({double size = 17}) => TextStyle(color: textMain, fontSize: size, fontWeight: FontWeight.w800, height: 1.25);

  TextStyle mutedStyle() => const TextStyle(color: textMuted, fontSize: 14, height: 1.4);

  RoundedRectangleBorder rounded(double radius) => RoundedRectangleBorder(borderRadius: BorderRadius.circular(radius), side: const BorderSide(color: Color(0x55C2C6D6)));

  String? inputHelper(String hint) {
    if (hint.contains('名称') || hint.contains('鍚嶇О')) return '显示在列表、聊天标题和头像上的名字。';
    if (hint.contains('定位') || hint.contains('瀹氫綅')) return '一句话概括身份，会作为模型提示词的一部分。';
    if (hint.contains('描述') || hint.contains('鎻忚堪')) return '写性格、背景、说话方式和互动边界。';
    if (hint.contains('开场') || hint.contains('寮€鍦')) return '新聊天里角色主动发出的第一句话。';
    return null;
  }

  InputDecoration inputDecoration(String hint, {IconData? icon, String? helper}) => InputDecoration(
        hintText: hint,
        helperText: helper ?? inputHelper(hint),
        helperMaxLines: 2,
        prefixIcon: icon == null ? null : Icon(icon),
        filled: true,
        fillColor: Colors.white,
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(16), borderSide: const BorderSide(color: Color(0xFFC2C6D6))),
        enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(16), borderSide: const BorderSide(color: Color(0xFFC2C6D6))),
        focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(16), borderSide: const BorderSide(color: blue, width: 1.4)),
      );
}
