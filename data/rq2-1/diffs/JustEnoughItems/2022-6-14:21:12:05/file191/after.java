package mezz.jei.common.input;

public interface IPaged {
	boolean nextPage();

	boolean previousPage();

	boolean hasNext();

	boolean hasPrevious();

	int getPageCount();

	int getPageNumber();
}
