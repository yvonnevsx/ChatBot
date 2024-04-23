# TechCase2 Chat App

TechCase2 is an Android chat application that uses speech recognition and text-to-speech
technologies to give an interactive chatting experience. The app allows users to speak their
messages into the app and receive responses in both text and spoken form.

## Features

- **Speech to Text**: Convert spoken words into text using Android's `SpeechRecognizer`.
- **Text to Speech**: Read out responses using Android's `TextToSpeech`.
- **Chat Interface**: Simple and intuitive interface for viewing chat history.
- **Quick Replies**: Offers a set of predefined quick replies to streamline conversation.
- **Animation Effects**: Utilizes animations to enhance user interactions.

## Requirements

- Android SDK 23 (Marshmallow) or higher
- Internet access for API interactions

## Permissions

- **RECORD_AUDIO**: Required to access the microphone for speech recognition.

## Setup

1. **Clone the repository**:
   ```bash
   git clone https://git.fhict.nl/I482348/sem4.git
   ```
2. **Import the project in Android Studio**:
    - Open Android Studio and select "Open an existing Android Studio project"
    - Navigate to the cloned repository and select it

3. **Build the project**:
    - Build > Make Project

4. **Run the app**:
    - Connect an Android device or use an emulator
    - Run > Run 'app'

## Usage

- Tap the microphone button to start recording your speech.
- Tap the send button or use the return key to send text messages.
- Select any quick reply to send predefined messages quickly.

## APIs Used

- OpenAI's GPT for generating chatbot responses. Ensure you replace the API key with your own.

## Libraries

- Android Support Libraries
- Gson for JSON parsing

## License

This project is licensed under the MIT License - see the LICENSE.md file for details.

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would
like to change.

Please make sure to update tests as appropriate.

## Author

- Yvonne van schie (yvonne.vanschie@hotmail.com)