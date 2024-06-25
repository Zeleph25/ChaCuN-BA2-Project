module ChaCuN {
    requires javafx.controls;
    requires java.net.http;

    exports ch.epfl.chacun;
    exports ch.epfl.chacun.gui;
    exports ch.epfl.chacun.extensions.json;
    exports ch.epfl.chacun.extensions.gui;
    exports ch.epfl.chacun.extensions.backend;
    exports ch.epfl.chacun.extensions.data;
    exports ch.epfl.chacun.extensions.bot;
}