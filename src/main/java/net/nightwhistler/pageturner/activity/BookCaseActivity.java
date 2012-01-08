package net.nightwhistler.pageturner.activity;

import net.nightwhistler.pageturner.R;
import net.nightwhistler.pageturner.animation.Animations;
import net.nightwhistler.pageturner.library.LibraryBook;
import net.nightwhistler.pageturner.library.LibraryService;
import net.nightwhistler.pageturner.library.QueryResult;
import net.nightwhistler.pageturner.view.BookCaseDrawable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import roboguice.activity.RoboActivity;
import roboguice.inject.InjectResource;
import roboguice.inject.InjectView;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ImageView;
import android.widget.ViewSwitcher;

import com.globalmentor.android.widget.VerifiedFlingListener;
import com.google.inject.Inject;

public class BookCaseActivity extends RoboActivity {
	
	private static final Logger LOG = LoggerFactory.getLogger(BookCaseActivity.class);
	
	@InjectView(R.id.bookCaseImage)
	private ImageView bookCaseView;
	
	@InjectView(R.id.bookCaseDummy)
	private ImageView dummyCaseView;	
	
	@InjectView(R.id.bookCaseViewSwitcher)
	private ViewSwitcher viewSwitcher;
	
	@InjectResource(R.drawable.pine)
	private Drawable background;
	
	@InjectResource(R.drawable.shelf)
	private Drawable shelf;
	
	@InjectResource(R.drawable.river_diary)
	private Drawable fallBackCover;
	
	private GestureDetector gestureDetector;
		
	private BookCaseDrawable bookCaseDrawable;
	private View.OnTouchListener gestureListener;
	
	@Inject
	private LibraryService libraryService;
	
	private LibraryBook selectedBook;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
				
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bookcase);
		
			
		QueryResult<LibraryBook> books = this.libraryService.findAllByTitle();
		this.bookCaseDrawable = new BookCaseDrawable(books);
		this.bookCaseDrawable.setBackground(background);
		this.bookCaseDrawable.setShelf(shelf);
		this.bookCaseDrawable.setFallBackCover(fallBackCover);
		
		this.bookCaseView.setImageDrawable(bookCaseDrawable);
		
		//this.bookCaseView.setImageDrawable(this.bookCaseDrawable);	
		
		this.gestureDetector = new GestureDetector(this, new ClickListener());
        this.gestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
            	LOG.debug("Got touch event: " + event );
                return gestureDetector.onTouchEvent(event);
            }
        };   
        
        
    	this.viewSwitcher.setOnTouchListener(gestureListener);
    	this.dummyCaseView.setOnTouchListener(gestureListener);
    	this.bookCaseView.setOnTouchListener(gestureListener);	
    	
    	registerForContextMenu(viewSwitcher);
    	registerForContextMenu(dummyCaseView);
    	registerForContextMenu(bookCaseView);
		
	}
	
	@Override
    public boolean onTouchEvent(MotionEvent event) {    	
    	return this.gestureDetector.onTouchEvent(event);
    }
	
	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if ( event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN 
				&& event.getAction() == KeyEvent.ACTION_DOWN ) {
			switchPage(true);
			return true;
		}
		
		if ( event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP 
				&& event.getAction() == KeyEvent.ACTION_DOWN ) {
			switchPage(false);
			return true;
		}
		
		return false;
	}
	
	public void switchPage( boolean right ) {
		
		int currentView = viewSwitcher.getDisplayedChild();		
		int otherView = Math.abs( currentView - 1 );
		
		int newOffset = 0;
		
		if (right ) {	
			viewSwitcher.setInAnimation( Animations.inFromRightAnimation() );
			viewSwitcher.setOutAnimation(Animations.outToLeftAnimation() );				
			
			newOffset = bookCaseDrawable.getStartOffset()
				+ bookCaseDrawable.getAmountOfBooks();
			
			if ( newOffset >= ( bookCaseDrawable.getBooks().getSize() -1) ) {
				newOffset = 0;
			}
		} else {
			viewSwitcher.setInAnimation(Animations.inFromLeftAnimation());
			viewSwitcher.setOutAnimation(Animations.outToRightAnimation() );
			
			newOffset = bookCaseDrawable.getStartOffset()
				- bookCaseDrawable.getCapacity();
			
			if ( newOffset < 0 ) {
				newOffset = (bookCaseDrawable.getBooks().getSize() -1) - newOffset;
			}
		}	
		
		ImageView imageView = (ImageView) viewSwitcher.getChildAt(otherView);
		imageView.setImageBitmap(null);
		imageView.setImageDrawable(null);
		
		BookCaseDrawable oldDrawable = this.bookCaseDrawable;
		oldDrawable.setDrawBooks(false);
		oldDrawable.getBooks().close();
		
		this.bookCaseDrawable = new BookCaseDrawable(libraryService.findAllByTitle());
		this.bookCaseDrawable.setBackground(background);
		this.bookCaseDrawable.setShelf(shelf);
		this.bookCaseDrawable.setFallBackCover(fallBackCover);
		this.bookCaseDrawable.setStartOffset(newOffset);

		Bitmap bitmap = Bitmap.createBitmap( bookCaseView.getWidth(),
				bookCaseView.getHeight(), Config.ARGB_8888 );
		Canvas canvas = new Canvas(bitmap);
		this.bookCaseDrawable.setBounds( 0, 0, bookCaseView.getWidth(), bookCaseView.getHeight() );
		this.bookCaseDrawable.draw(canvas);
		
		imageView.setImageBitmap(bitmap);
		
		
		viewSwitcher.showNext();
	}	
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {		
		
		MenuItem detailsItem = menu.add( "View details");
		
		detailsItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				Intent intent = new Intent( BookCaseActivity.this, BookDetailsActivity.class );
				intent.putExtra("book", selectedBook.getFileName() );				
				startActivity(intent);					
				return true;
			}
		});
		/*
		MenuItem deleteItem = menu.add("Delete from library");
		
		deleteItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				libraryService.deleteBook( selectedBook.getFileName() );
				new LoadBooksTask().execute(lastPosition);
				return true;					
			}
		});				
		*/
	}	
	
	
	private class ClickListener extends VerifiedFlingListener {
		
		public ClickListener() {
			super(BookCaseActivity.this);
		}
		
		@Override
		public void onLongPress(MotionEvent e) {
			selectedBook = bookCaseDrawable.findBookAtLocation(e.getX(), e.getY());
			openContextMenu(viewSwitcher);
		}		
		
		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			LibraryBook book = bookCaseDrawable.findBookAtLocation(e.getX(), e.getY());
			
			if ( book != null ) {
				Intent intent = new Intent(BookCaseActivity.this, ReadingActivity.class);
			
				intent.setData( Uri.parse(book.getFileName()));
				setResult(RESULT_OK, intent);
				
				startActivityIfNeeded(intent, 99);
			
				return true;
			}
			
			return false;
		}
		
		@Override
		public boolean onVerifiedFling(MotionEvent e1, MotionEvent e2,
				float velocityX, float velocityY) {
						
			LOG.debug("Fling confirmed.");
			
			switchPage( velocityX < 0 );
			
			return true;
		}
		
	}
	
	
}
