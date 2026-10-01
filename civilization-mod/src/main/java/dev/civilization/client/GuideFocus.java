package dev.civilization.client;

/** Short release delay smooths boundary jitter without delaying initial acquisition. */
final class GuideFocus {
    private long lastSeen = Long.MIN_VALUE;
    private boolean visible;
    boolean visible(){return visible;}
    boolean update(boolean aimed,long now){
        if(aimed){lastSeen=now;visible=true;}
        else visible=lastSeen!=Long.MIN_VALUE && now>=lastSeen && now-lastSeen<150;
        return visible;
    }
    void reset(){lastSeen=Long.MIN_VALUE;visible=false;}
}
