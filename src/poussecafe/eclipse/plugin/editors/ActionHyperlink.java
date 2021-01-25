package poussecafe.eclipse.plugin.editors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;

import static java.util.Objects.requireNonNull;

public class ActionHyperlink implements IHyperlink {

    @Override
    public IRegion getHyperlinkRegion() {
        return region;
    }

    private IRegion region;

    @Override
    public String getTypeLabel() {
        return null;
    }

    @Override
    public String getHyperlinkText() {
        return "Open " + name;
    }

    private String name;

    @Override
    public void open() {
        action.run();
    }

    private Action action;

    public static class Builder {

        private ActionHyperlink link = new ActionHyperlink();

        public ActionHyperlink build() {
            requireNonNull(link.region);
            requireNonNull(link.name);
            requireNonNull(link.action);
            return link;
        }

        public Builder region(IRegion region) {
            link.region = region;
            return this;
        }

        public Builder name(String name) {
            link.name = name;
            return this;
        }

        public Builder action(Action action) {
            link.action = action;
            return this;
        }
    }

    private ActionHyperlink() {

    }
}
