package org.flowable.dmn.model;

/**
 * @author Yvo Swillens
 */
public class DmnElementReference {

    protected String href;
    protected String parsedId;

    public String getHref() {
        return href;
    }
    public void setHref(String href) {
        this.href = href;
    }

    /**
     * helper method returning the href with the starting '#'
     * @return
     */
    public String getParsedId() {
        if (href != null || href.length() > 1) {
            return href.substring(1, href.length());
        }
        return null;
    }
}
