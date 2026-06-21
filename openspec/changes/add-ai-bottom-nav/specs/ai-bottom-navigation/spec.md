## ADDED Requirements

### Requirement: Bottom Navigation Uses Home AI Mine
The app SHALL expose exactly three bottom navigation destinations for top-level use: 首页, AI, and 我的.

#### Scenario: Top-level bottom tabs are visible
- **GIVEN** the user is on a top-level screen
- **WHEN** the screen is displayed
- **THEN** the bottom navigation shows 首页, AI, and 我的
- **AND** it does not show History or Settings as bottom tabs

#### Scenario: Mine preserves secondary navigation
- **GIVEN** the user opens 我的
- **WHEN** the Mine screen is displayed
- **THEN** the user can navigate to Settings and History from that screen

### Requirement: AI Page Starts With Podcast Selection
The AI page SHALL require a podcast session selection before starting a conversation when no AI conversation target is selected.

#### Scenario: First AI entry shows podcast cards
- **GIVEN** podcast cards are available
- **AND** no AI conversation target has been selected
- **WHEN** the user opens AI
- **THEN** the AI page shows horizontally swipeable podcast cards
- **AND** selecting a podcast starts a conversation scoped to that podcast

#### Scenario: No podcast content exists
- **GIVEN** no podcast cards are available
- **WHEN** the user opens AI
- **THEN** the AI page shows an empty state instead of an empty chat

### Requirement: AI Page Supports New Conversation
The AI page SHALL provide a top-right action to start a new conversation.

#### Scenario: Start new conversation
- **GIVEN** the AI page has an active podcast conversation
- **WHEN** the user taps the top-right new conversation action
- **THEN** the AI page returns to the podcast card chooser
- **AND** the previous selected podcast is cleared

### Requirement: AI Page Uses Chat Layout
The AI page SHALL present a WeChat-like conversation between the user and AI assistant after a podcast is selected.

#### Scenario: Selected podcast shows chat
- **GIVEN** the user selected a podcast card on the AI page
- **WHEN** the conversation is displayed
- **THEN** assistant messages are visually grouped on the left
- **AND** user messages are visually grouped on the right
- **AND** the input row allows the user to enter and send a message
