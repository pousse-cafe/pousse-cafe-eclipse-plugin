package poussecafe.eclipse.plugin.actions;

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.ui.actions.OpenAction;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchSite;

import static java.util.Objects.requireNonNull;

public class OpenJavaEditorAction extends Action {

    @Override
    public void run() {
        new OpenAction(site).run(new StructuredSelection(member));
    }

    private IWorkbenchSite site;

    private IMember member;

    public static class Builder {

        public OpenJavaEditorAction build() {
            requireNonNull(action.site);
            requireNonNull(action.member);
            return action;
        }

        private OpenJavaEditorAction action = new OpenJavaEditorAction();

        public Builder site(IWorkbenchSite site) {
            action.site = site;
            return this;
        }

        public Builder member(IMember member) {
            action.member = member;
            return this;
        }
    }

    private OpenJavaEditorAction() {

    }
}
