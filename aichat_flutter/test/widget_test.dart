import 'package:flutter_test/flutter_test.dart';

import 'package:aichat_flutter/main.dart';

void main() {
  testWidgets('AI chat app renders main tabs', (WidgetTester tester) async {
    await tester.pumpWidget(const AiChatApp());

    expect(find.text('聊天'), findsWidgets);
    expect(find.text('角色'), findsWidgets);
    expect(find.text('我的'), findsWidgets);
    expect(find.text('AI Chat Hub'), findsOneWidget);
  });
}
