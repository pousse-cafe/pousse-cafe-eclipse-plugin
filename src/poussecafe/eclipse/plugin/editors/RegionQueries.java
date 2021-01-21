package poussecafe.eclipse.plugin.editors;

import org.eclipse.jface.text.IRegion;

public class RegionQueries {

    public RegionQueries(IRegion region) {
        this.region = region;
    }

    private IRegion region;

    public boolean contains(IRegion otherRegion) {
        return otherRegion.getOffset() >= region.getOffset()
                && otherRegion.getOffset() + otherRegion.getLength() <= region.getOffset() + region.getLength();
    }

    public boolean contains(RegionQueries otherRegion) {
        return contains(otherRegion.region);
    }
}
