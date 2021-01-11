package poussecafe.eclipse.plugin.editors;

import org.eclipse.jface.text.rules.DefaultDamagerRepairer;

public class EmilDamagerRepairer extends DefaultDamagerRepairer {

    public EmilDamagerRepairer(Style style) {
        super(new EmilScanner(style));
    }
}
