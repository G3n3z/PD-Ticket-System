# Note: Coloca-me na pasta onde está o ficheiro do client .jar

java --module-path javafx-sdk-19/lib/ --add-modules javafx.base,javafx.controls,javafx.fxml,javafx.graphics,javafx.media,javafx.swing,javafx.web -jar PD-Ticket-System.jar

## Old commands for historic
# java --module-path /Users/diogo/Documents/Java_SDKs/javafx-sdk-19/lib/ --add-modules javafx.base,javafx.controls,javafx.fxml,javafx.graphics,javafx.media,javafx.swing,javafx.web -jar PD-Ticket-System.jar
# javac --module-path /Users/diogo/Documents/Java_SDKs/javafx-sdk-19/lib/ --add-modules javafx.base,javafx.controls,javafx.fxml,javafx.graphics,javafx.media,javafx.swing,javafx.web -sourcepath ./src/ -d ./out/ ./src/main/java/com/isec/pd22/client/ui/MainJavaFX.java