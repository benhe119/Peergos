package peergos.shared.user;

import jsinterop.annotations.JsProperty;

import java.time.LocalDateTime;

public class TimelineEntry {
    @JsProperty
    public final String friendName;
    @JsProperty
    public final LocalDateTime created;
    @JsProperty
    public final String filename;
    @JsProperty
    public final boolean isDrectory;
    @JsProperty
    public final boolean writeAccess;
    public TimelineEntry(String friendName, LocalDateTime created, String filename, boolean isDirectory, boolean writeAccess) {
        this.friendName = friendName;
        this.created = created;
        this.filename = filename;
        this.isDrectory = isDirectory;
        this.writeAccess = writeAccess;
    }
}
