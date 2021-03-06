package org.infinispan.test.fwk;

/**
 * Flags that allow JGroups transport stack to be tweaked depending on the test
 * case requirements. For example, you can remove failure detection, or remove
 * merge protocol...etc.
 *
 * @author Galder Zamarreño
 * @since 5.1
 */
public class TransportFlags {

    private boolean withFD;
    private boolean withMerge;
    private boolean withReplay2 = true;
    private int sideIndex = -1;
    private int nodeIndex = -1;
    private String siteName;
    private String relayConfig;

    public TransportFlags withFD(boolean withFD) {
        this.withFD = withFD;
        return this;
    }

    public boolean withFD() {
        return withFD;
    }

    public TransportFlags withMerge(boolean withMerge) {
        this.withMerge = withMerge;
        return this;
    }

    public boolean withMerge() {
        return withMerge;
    }

    public TransportFlags withReplay2(boolean withReplay2){
        this.withReplay2 = withReplay2;
        return this;
    }

    public boolean withReplay2(){
        return withReplay2;
    }

    public TransportFlags withSiteIndex(int siteIndex) {
        this.sideIndex = siteIndex;
        return this;
    }

    public TransportFlags withNodeIndex(int nodeIndex) {
        this.nodeIndex = nodeIndex;
        return this;
    }

    public TransportFlags withSiteName(String siteName) {
        this.siteName = siteName;
        return this;
    }

    public TransportFlags withRelayConfig(String relayConf) {
        this.relayConfig = relayConf;
        return this;
    }

    public String siteName() {
        return siteName;
    }

    public String relayConfig() {
        return relayConfig;
    }

    public int siteIndex() {
        return sideIndex;
    }

    public int nodeIndex() {
        return nodeIndex;
    }

    public boolean isSiteIndexSpecified() {
        return siteIndex() >= 0;
    }
}
