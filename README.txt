MART -- Music Analysis, Recognition and Transcription framework is aimed at creating tools to extract high-level information (chords, key, rhythm) from music signals.

Installation instructions.

Requirements:

1. Sun JDK 1.6
2. Maven 2.2
3. HTK (http://htk.eng.cam.ac.uk/)
4. SRILM (http://www.speech.sri.com/projects/srilm/)


Installation
1. Create $JAVA_HOME variable and set it to your current JDK installation path.
2. Create $M2_HOME variable and set it to your current maven installation path. Add $M2_HOME/bin to the $PATH variable
3. Follow the instructions of HTK and SRILM toolkits. Add the compiled binaries to $PATH.
4. Add trainLMHVite script from bin/ to $PATH
5. mvn package

