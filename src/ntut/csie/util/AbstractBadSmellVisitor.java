package ntut.csie.util;

import java.util.List;
import ntut.csie.csdet.data.MarkerInfo;
import org.eclipse.jdt.core.dom.ASTVisitor;

public abstract class AbstractBadSmellVisitor extends ASTVisitor {
	
	public abstract List<MarkerInfo> getBadSmellCollected();
}
