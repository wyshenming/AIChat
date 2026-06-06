package com.aichathub.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends Activity {
    private static final int BACKGROUND = Color.rgb(248, 249, 255);
    private static final int SURFACE = Color.WHITE;
    private static final int SURFACE_LOW = Color.rgb(239, 244, 255);
    private static final int PRIMARY = Color.rgb(59, 130, 246);
    private static final int PRIMARY_DARK = Color.rgb(0, 88, 190);
    private static final int TEXT = Color.rgb(18, 28, 42);
    private static final int MUTED = Color.rgb(66, 71, 84);
    private static final int OUTLINE = Color.rgb(194, 198, 214);
    private static final int SUCCESS = Color.rgb(34, 197, 94);

    private LinearLayout root;
    private LinearLayout content;
    private LinearLayout tabBar;
    private int activeTab = 0;
    private int activeChat = 0;
    private final List<Chat> chats = new ArrayList<>();
    private final List<CharacterProfile> characters = new ArrayList<>();
    private final List<ApiProfile> apis = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        seedData();
        getWindow().setStatusBarColor(BACKGROUND);
        buildShell();
        showChats();
    }

    private void buildShell() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BACKGROUND);
        setContentView(root);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        root.addView(content, new LinearLayout.LayoutParams(match(), 0, 1));

        tabBar = new LinearLayout(this);
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        tabBar.setGravity(Gravity.CENTER);
        tabBar.setPadding(dp(8), dp(8), dp(8), dp(10));
        tabBar.setBackgroundColor(SURFACE);
        root.addView(tabBar, new LinearLayout.LayoutParams(match(), dp(72)));
        renderTabs();
    }

    private void renderTabs() {
        tabBar.removeAllViews();
        addTab("聊天", "●", 0);
        addTab("角色", "◆", 1);
        addTab("API", "◇", 2);
        addTab("我的", "◉", 3);
    }

    private void addTab(String label, String icon, int index) {
        TextView tab = text(icon + "\n" + label, 12, index == activeTab ? PRIMARY : MUTED, Typeface.BOLD);
        tab.setGravity(Gravity.CENTER);
        tab.setOnClickListener(v -> {
            activeTab = index;
            renderTabs();
            if (index == 0) showChats();
            if (index == 1) showCharacters();
            if (index == 2) showApiManagement();
            if (index == 3) showMe();
        });
        tabBar.addView(tab, new LinearLayout.LayoutParams(0, match(), 1));
    }

    private void showChats() {
        activeTab = 0;
        renderTabs();
        content.removeAllViews();
        content.addView(header("消息", "安静有序的 AI 对话", "+", v -> showChatRoom(activeChat)));

        EditText search = input("搜索会话或角色");
        search.setSingleLine(true);
        search.setBackground(bg(SURFACE_LOW, 16, 0));
        LinearLayout.LayoutParams searchLp = margins(match(), dp(48), dp(16), 0, dp(16), dp(12));
        content.addView(search, searchLp);

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = column();
        list.setPadding(dp(16), 0, dp(16), dp(16));
        scroll.addView(list);
        content.addView(scroll, new LinearLayout.LayoutParams(match(), 0, 1));

        for (int i = 0; i < chats.size(); i++) {
            Chat chat = chats.get(i);
            LinearLayout row = row();
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(10), dp(12), dp(10));
            row.setMinimumHeight(dp(76));
            row.setBackground(bg(i == activeChat ? SURFACE_LOW : Color.TRANSPARENT, 16, 0));
            int index = i;
            row.setOnClickListener(v -> {
                activeChat = index;
                showChatRoom(index);
            });

            row.addView(avatar(chat.initial, chat.avatarColor, true), new LinearLayout.LayoutParams(dp(50), dp(50)));
            LinearLayout meta = column();
            meta.setPadding(dp(12), 0, 0, 0);
            TextView name = text(chat.name, 16, TEXT, Typeface.BOLD);
            TextView preview = text(chat.preview(), 13, MUTED, Typeface.NORMAL);
            meta.addView(name);
            meta.addView(preview);
            row.addView(meta, new LinearLayout.LayoutParams(0, wrap(), 1));
            TextView time = text(chat.time, 12, MUTED, Typeface.NORMAL);
            row.addView(time);
            list.addView(row, margins(match(), wrap(), 0, 0, 0, dp(8)));
        }
    }

    private void showChatRoom(int index) {
        activeChat = index;
        Chat chat = chats.get(index);
        content.removeAllViews();
        content.addView(chatHeader(chat));

        ScrollView scroll = new ScrollView(this);
        LinearLayout messages = column();
        messages.setPadding(dp(16), dp(12), dp(16), dp(12));
        scroll.addView(messages);
        content.addView(scroll, new LinearLayout.LayoutParams(match(), 0, 1));

        for (Message message : chat.messages) {
            messages.addView(messageRow(message));
        }

        HorizontalScrollView chipsScroller = new HorizontalScrollView(this);
        chipsScroller.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = row();
        chips.setPadding(dp(16), dp(4), dp(16), dp(8));
        chipsScroller.addView(chips);
        for (String quick : Arrays.asList("总结", "翻译", "语气更温和", "提取待办")) {
            TextView chip = text(quick, 12, PRIMARY_DARK, Typeface.BOLD);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(14), 0, dp(14), 0);
            chip.setBackground(bg(SURFACE_LOW, 999, 0));
            chips.addView(chip, margins(wrap(), dp(34), 0, 0, dp(8), 0));
        }
        content.addView(chipsScroller, new LinearLayout.LayoutParams(match(), dp(48)));

        LinearLayout composer = row();
        composer.setGravity(Gravity.CENTER_VERTICAL);
        composer.setPadding(dp(12), dp(8), dp(12), dp(12));
        composer.setBackgroundColor(SURFACE);
        EditText input = input("发送给 " + chat.name);
        input.setSingleLine(true);
        input.setBackground(bg(SURFACE_LOW, 16, 0));
        composer.addView(input, new LinearLayout.LayoutParams(0, dp(48), 1));
        Button send = button("发送", PRIMARY, Color.WHITE);
        send.setOnClickListener(v -> {
            String value = input.getText().toString().trim();
            if (value.isEmpty()) return;
            chat.messages.add(new Message(value, true));
            chat.messages.add(new Message("收到。我先给你一个更清晰、更平和的版本：" + value, false));
            chat.time = "刚刚";
            showChatRoom(activeChat);
        });
        composer.addView(send, margins(dp(78), dp(48), dp(8), 0, 0, 0));
        content.addView(composer, new LinearLayout.LayoutParams(match(), dp(68)));
    }

    private View chatHeader(Chat chat) {
        LinearLayout header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(12), dp(48), dp(12), dp(10));
        header.setBackgroundColor(BACKGROUND);

        TextView back = text("‹", 34, PRIMARY_DARK, Typeface.NORMAL);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> showChats());
        header.addView(back, new LinearLayout.LayoutParams(dp(38), dp(54)));

        header.addView(avatar(chat.initial, chat.avatarColor, true), new LinearLayout.LayoutParams(dp(44), dp(44)));
        LinearLayout title = column();
        title.setPadding(dp(10), 0, 0, 0);
        title.addView(text(chat.name, 18, TEXT, Typeface.BOLD));
        title.addView(text(chat.role, 12, MUTED, Typeface.NORMAL));
        header.addView(title, new LinearLayout.LayoutParams(0, wrap(), 1));

        TextView menu = text("⋯", 28, MUTED, Typeface.BOLD);
        menu.setGravity(Gravity.CENTER);
        header.addView(menu, new LinearLayout.LayoutParams(dp(44), dp(54)));
        return header;
    }

    private View messageRow(Message message) {
        LinearLayout wrapper = row();
        wrapper.setGravity(message.outgoing ? Gravity.RIGHT : Gravity.LEFT);
        wrapper.setPadding(0, dp(4), 0, dp(4));

        TextView bubble = text(message.text, 15, message.outgoing ? Color.WHITE : TEXT, Typeface.NORMAL);
        bubble.setLineSpacing(dp(2), 1f);
        bubble.setPadding(dp(14), dp(10), dp(14), dp(10));
        bubble.setBackground(bg(message.outgoing ? PRIMARY : SURFACE, 16, message.outgoing ? 0 : Color.rgb(241, 245, 249)));
        wrapper.addView(bubble, new LinearLayout.LayoutParams((int) (getResources().getDisplayMetrics().widthPixels * 0.72f), wrap()));
        return wrapper;
    }

    private void showCharacters() {
        content.removeAllViews();
        content.addView(header("角色管理", "可复用的 AI 助手角色", "新建", v -> {
            characters.add(new CharacterProfile("研究搭档", "把零散问题整理成结构化结论。", "#"));
            showCharacters();
        }));

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = column();
        list.setPadding(dp(16), dp(8), dp(16), dp(18));
        scroll.addView(list);
        content.addView(scroll, new LinearLayout.LayoutParams(match(), 0, 1));

        for (CharacterProfile profile : characters) {
            LinearLayout card = column();
            card.setPadding(dp(16), dp(14), dp(16), dp(14));
            card.setBackground(bg(SURFACE, 18, Color.rgb(233, 238, 252)));
            LinearLayout top = row();
            top.setGravity(Gravity.CENTER_VERTICAL);
            top.addView(avatar(profile.initial, PRIMARY, false), new LinearLayout.LayoutParams(dp(44), dp(44)));
            LinearLayout copy = column();
            copy.setPadding(dp(12), 0, 0, 0);
            copy.addView(text(profile.name, 17, TEXT, Typeface.BOLD));
            copy.addView(text(profile.summary, 13, MUTED, Typeface.NORMAL));
            top.addView(copy, new LinearLayout.LayoutParams(0, wrap(), 1));
            card.addView(top);
            TextView prompt = text("系统风格：简洁、温和、实用。点击后可在下一次对话中使用这个角色。", 13, MUTED, Typeface.NORMAL);
            prompt.setPadding(0, dp(12), 0, 0);
            card.addView(prompt);
            card.setOnClickListener(v -> Toast.makeText(this, "已选择：" + profile.name, Toast.LENGTH_SHORT).show());
            list.addView(card, margins(match(), wrap(), 0, 0, 0, dp(12)));
        }
    }

    private void showApiManagement() {
        content.removeAllViews();
        content.addView(header("API 管理", "模型服务商与密钥配置", "添加", v -> {
            apis.add(new ApiProfile("自定义 OpenAI 兼容接口", "https://api.example.com/v1", "未验证"));
            showApiManagement();
        }));

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = column();
        list.setPadding(dp(16), dp(8), dp(16), dp(18));
        scroll.addView(list);
        content.addView(scroll, new LinearLayout.LayoutParams(match(), 0, 1));

        for (ApiProfile api : apis) {
            LinearLayout card = column();
            card.setPadding(dp(16), dp(16), dp(16), dp(16));
            card.setBackground(bg(SURFACE, 18, Color.rgb(233, 238, 252)));
            card.addView(text(api.name, 17, TEXT, Typeface.BOLD));
            card.addView(text(api.endpoint, 13, MUTED, Typeface.NORMAL));
            LinearLayout status = row();
            status.setGravity(Gravity.CENTER_VERTICAL);
            TextView dot = text("●", 14, api.status.equals("已启用") ? SUCCESS : PRIMARY, Typeface.BOLD);
            status.addView(dot);
            TextView statusText = text("  " + api.status, 13, MUTED, Typeface.BOLD);
            status.addView(statusText);
            card.addView(status, margins(match(), wrap(), 0, dp(10), 0, 0));

            LinearLayout actions = row();
            Button test = button("测试", SURFACE_LOW, PRIMARY_DARK);
            test.setOnClickListener(v -> Toast.makeText(this, api.name + " 连接测试已模拟完成", Toast.LENGTH_SHORT).show());
            actions.addView(test, new LinearLayout.LayoutParams(0, dp(44), 1));
            Button edit = button("编辑", PRIMARY, Color.WHITE);
            edit.setOnClickListener(v -> Toast.makeText(this, "编辑 " + api.name, Toast.LENGTH_SHORT).show());
            actions.addView(edit, margins(0, dp(44), dp(10), 0, 0, 0, 1));
            card.addView(actions, margins(match(), wrap(), 0, dp(12), 0, 0));
            list.addView(card, margins(match(), wrap(), 0, 0, 0, dp(12)));
        }
    }

    private void showMe() {
        content.removeAllViews();
        content.addView(header("我的", "个人资料与工作区", "编辑", v -> showProfileEditor()));

        ScrollView scroll = new ScrollView(this);
        LinearLayout body = column();
        body.setPadding(dp(16), dp(10), dp(16), dp(18));
        scroll.addView(body);
        content.addView(scroll, new LinearLayout.LayoutParams(match(), 0, 1));

        LinearLayout profile = row();
        profile.setGravity(Gravity.CENTER_VERTICAL);
        profile.setPadding(dp(18), dp(18), dp(18), dp(18));
        profile.setBackground(bg(SURFACE, 22, Color.rgb(233, 238, 252)));
        profile.addView(avatar("陈", PRIMARY, true), new LinearLayout.LayoutParams(dp(64), dp(64)));
        LinearLayout copy = column();
        copy.setPadding(dp(14), 0, 0, 0);
        copy.addView(text("陈亦安", 20, TEXT, Typeface.BOLD));
        copy.addView(text("个人 AI 工作区", 13, MUTED, Typeface.NORMAL));
        profile.addView(copy, new LinearLayout.LayoutParams(0, wrap(), 1));
        body.addView(profile, margins(match(), wrap(), 0, 0, 0, dp(14)));

        body.addView(settingsRow("收藏提示词", "24 条片段"));
        body.addView(settingsRow("会话记忆", "已开启"));
        body.addView(settingsRow("主题", "静谧连接"));
        body.addView(settingsRow("存储", "本地演示模式"));
    }

    private void showProfileEditor() {
        content.removeAllViews();
        content.addView(header("资料编辑", "更新你的公开聊天身份", "保存", v -> {
            Toast.makeText(this, "资料已保存", Toast.LENGTH_SHORT).show();
            showMe();
        }));
        LinearLayout form = column();
        form.setPadding(dp(16), dp(14), dp(16), 0);
        content.addView(form);
        form.addView(label("显示名称"));
        form.addView(input("陈亦安"), margins(match(), dp(52), 0, dp(6), 0, dp(14)));
        form.addView(label("个人简介"));
        EditText bio = input("构建安静高效的 AI 工作流");
        bio.setMinLines(4);
        bio.setGravity(Gravity.TOP);
        bio.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        form.addView(bio, margins(match(), dp(120), 0, dp(6), 0, dp(14)));
        form.addView(label("默认模型"));
        form.addView(input("GPT-4.1 兼容模型"), margins(match(), dp(52), 0, dp(6), 0, dp(14)));
    }

    private View header(String title, String subtitle, String action, View.OnClickListener listener) {
        LinearLayout header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(52), dp(16), dp(12));
        header.setBackgroundColor(BACKGROUND);
        LinearLayout copy = column();
        copy.addView(text(title, 28, TEXT, Typeface.BOLD));
        copy.addView(text(subtitle, 13, MUTED, Typeface.NORMAL));
        header.addView(copy, new LinearLayout.LayoutParams(0, wrap(), 1));
        Button button = button(action, PRIMARY, Color.WHITE);
        button.setOnClickListener(listener);
        header.addView(button, new LinearLayout.LayoutParams(dp(action.length() > 3 ? 72 : 50), dp(42)));
        return header;
    }

    private View settingsRow(String title, String detail) {
        LinearLayout row = row();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));
        row.setBackground(bg(SURFACE, 16, Color.rgb(233, 238, 252)));
        LinearLayout copy = column();
        copy.addView(text(title, 16, TEXT, Typeface.BOLD));
        copy.addView(text(detail, 13, MUTED, Typeface.NORMAL));
        row.addView(copy, new LinearLayout.LayoutParams(0, wrap(), 1));
        row.addView(text("›", 26, MUTED, Typeface.NORMAL));
        return withMargins(row, match(), wrap(), 0, 0, 0, dp(10));
    }

    private TextView label(String value) {
        TextView label = text(value, 12, MUTED, Typeface.BOLD);
        label.setAllCaps(true);
        return label;
    }

    private EditText input(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setTextColor(TEXT);
        input.setHintTextColor(Color.rgb(139, 148, 163));
        input.setTextSize(15);
        input.setPadding(dp(14), 0, dp(14), 0);
        input.setBackground(bg(SURFACE, 16, Color.rgb(233, 238, 252)));
        return input;
    }

    private Button button(String label, int background, int foreground) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(foreground);
        button.setTextSize(13);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setBackground(bg(background, 16, 0));
        return button;
    }

    private View avatar(String value, int color, boolean online) {
        TextView avatar = text(value, 18, Color.WHITE, Typeface.BOLD);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(bg(color, 999, 0));
        if (!online) return avatar;
        FrameLayout frame = new FrameLayout(this);
        frame.addView(avatar, new FrameLayout.LayoutParams(match(), match()));
        TextView dot = new TextView(this);
        dot.setBackground(bg(SUCCESS, 999, Color.WHITE));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(13), dp(13), Gravity.RIGHT | Gravity.BOTTOM);
        lp.setMargins(0, 0, dp(1), dp(1));
        frame.addView(dot, lp);
        return frame;
    }

    private TextView text(String value, int sp, int color, int style) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(sp);
        text.setTextColor(color);
        text.setTypeface(Typeface.DEFAULT, style);
        return text;
    }

    private GradientDrawable bg(int color, int radiusDp, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeColor != 0) drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout row() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        return layout;
    }

    private View withMargins(View view, int width, int height, int left, int top, int right, int bottom) {
        view.setLayoutParams(margins(width, height, left, top, right, bottom));
        return view;
    }

    private LinearLayout.LayoutParams margins(int width, int height, int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, height);
        lp.setMargins(left, top, right, bottom);
        return lp;
    }

    private LinearLayout.LayoutParams margins(int width, int height, int left, int top, int right, int bottom, float weight) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, height, weight);
        lp.setMargins(left, top, right, bottom);
        return lp;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int match() {
        return ViewGroup.LayoutParams.MATCH_PARENT;
    }

    private int wrap() {
        return ViewGroup.LayoutParams.WRAP_CONTENT;
    }

    private void seedData() {
        Chat design = new Chat("设计搭档", "产品设计角色", "设", Color.rgb(59, 130, 246), "09:42");
        design.messages.add(new Message("可以总结一下 Stitch 里的页面吗？", true));
        design.messages.add(new Message("一共有六个核心界面：聊天列表、聊天详情、角色管理、API 管理、个人中心和资料编辑。整体应该保持安静、蓝色、移动端优先。", false));
        design.messages.add(new Message("把它整理成构建计划。", true));
        design.messages.add(new Message("先做可用的聊天外壳，再逐步加入角色管理、服务商配置和个人工作区。", false));
        chats.add(design);

        Chat writer = new Chat("写作伙伴", "语气与改写角色", "写", Color.rgb(96, 165, 250), "昨天");
        writer.messages.add(new Message("把这段产品说明改得更温和。", true));
        writer.messages.add(new Message("没问题。我会保持简洁，同时让表达更有人味，也不丢掉关键操作信息。", false));
        chats.add(writer);

        Chat api = new Chat("接口助手", "服务商配置角色", "接", Color.rgb(0, 96, 172), "周一");
        api.messages.add(new Message("API 配置需要哪些字段？", true));
        api.messages.add(new Message("第一版保留名称、接口地址、模型、密钥掩码、默认温度和连接状态就足够清晰。", false));
        chats.add(api);

        characters.add(new CharacterProfile("设计搭档", "把粗略想法整理成清晰的产品界面。", "设"));
        characters.add(new CharacterProfile("写作伙伴", "按语气和意图改写内容。", "写"));
        characters.add(new CharacterProfile("接口助手", "解释服务商接入和模型配置。", "接"));

        apis.add(new ApiProfile("OpenAI", "https://api.openai.com/v1", "已启用"));
        apis.add(new ApiProfile("本地兼容接口", "http://10.0.2.2:11434/v1", "草稿"));
    }

    private static class Chat {
        final String name;
        final String role;
        final String initial;
        final int avatarColor;
        String time;
        final List<Message> messages = new ArrayList<>();

        Chat(String name, String role, String initial, int avatarColor, String time) {
            this.name = name;
            this.role = role;
            this.initial = initial;
            this.avatarColor = avatarColor;
            this.time = time;
        }

        String preview() {
            if (messages.isEmpty()) return "";
            return messages.get(messages.size() - 1).text;
        }
    }

    private static class Message {
        final String text;
        final boolean outgoing;

        Message(String text, boolean outgoing) {
            this.text = text;
            this.outgoing = outgoing;
        }
    }

    private static class CharacterProfile {
        final String name;
        final String summary;
        final String initial;

        CharacterProfile(String name, String summary, String initial) {
            this.name = name;
            this.summary = summary;
            this.initial = initial;
        }
    }

    private static class ApiProfile {
        final String name;
        final String endpoint;
        final String status;

        ApiProfile(String name, String endpoint, String status) {
            this.name = name;
            this.endpoint = endpoint;
            this.status = status;
        }
    }
}
