package poussecafe.eclipse.plugin.builder;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IJavaProject;

public class PousseCafeNature implements IProjectNature {

    public static boolean isPousseCafeProject(IProject project) {
        try {
            return project.hasNature(PousseCafeNature.NATURE_ID);
        } catch (CoreException e) {
            Platform.getLog(PousseCafeNature.class).error("Unable to detect project nature " + project.getName(), e);
            return false;
        }
    }

    public static final String NATURE_ID = "poussecafe.eclipse.plugin.pousseCafeNature";

    public static boolean isPousseCafeProject(IJavaProject project) {
        return isPousseCafeProject(project.getProject());
    }

    private IProject project;

    @Override
    public void configure() throws CoreException {
        IProjectDescription desc = project.getDescription();
        ICommand[] commands = desc.getBuildSpec();

        for (int i = 0; i < commands.length; ++i) {
            if(commands[i].getBuilderName().equals(PousseCafeBuilder.BUILDER_ID)) {
                return;
            }
        }

        ICommand[] newCommands = new ICommand[commands.length + 1];
        System.arraycopy(commands, 0, newCommands, 0, commands.length);
        ICommand command = desc.newCommand();
        command.setBuilderName(PousseCafeBuilder.BUILDER_ID);
        newCommands[newCommands.length - 1] = command;
        desc.setBuildSpec(newCommands);
        project.setDescription(desc, null);
    }

    @Override
    public void deconfigure() throws CoreException {
        IProjectDescription description = getProject().getDescription();
        ICommand[] commands = description.getBuildSpec();
        for (int i = 0; i < commands.length; ++i) {
            if(commands[i].getBuilderName().equals(PousseCafeBuilder.BUILDER_ID)) {
                ICommand[] newCommands = new ICommand[commands.length - 1];
                System.arraycopy(commands, 0, newCommands, 0, i);
                System.arraycopy(commands, i + 1, newCommands, i, commands.length - i - 1);
                description.setBuildSpec(newCommands);
                project.setDescription(description, null);
                return;
            }
        }
    }

    @Override
    public IProject getProject() {
        return project;
    }

    @Override
    public void setProject(IProject project) {
        this.project = project;
    }
}
