package org.bharathi.controller;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;
import javax.websocket.Session;

import org.bharathi.model.BookIssue;
import org.bharathi.model.User;
import org.bharathi.model.Book;
import org.bharathi.service.IBookIssueService;
import org.bharathi.service.IBooksService;
import org.bharathi.service.IUserService;
import org.bharathi.service.ServiceFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/booksissue")
public class BookIssueController {

	
	@Autowired
	private IBookIssueService service;
	
	@Autowired
	private IBooksService bookservice;
	
	@Autowired
	private IUserService userservice;
	
	Logger logger=LoggerFactory.getLogger(BookIssueController.class);
	
	@GetMapping("/issuebookform")
	public String bookForm(Map<String,Object> model)
	
	{
		SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
		String formattedDate = df.format(new Date());
		
		
		BookIssue bookissue=new BookIssue();
		bookissue.setIssueDate(formattedDate);
		
		model.put("bookissue",bookissue);
		
		return "issuebook_form";
	}
	
	
	
	
	@PostMapping("/issuebook")
	public String issueBook(@ModelAttribute BookIssue bookissue,HttpSession session)
	
	{
		Integer noofcopies;
		
	    Integer stdId=bookissue.getStdId();
	    System.out.println(stdId);
	    
	    boolean stdExist=userservice.isUserExist(stdId);
	    System.out.println(stdExist);
	    
	    if(stdExist==false)
	    {
	    	System.out.println("Student not found");
       	 session.setAttribute("invaliduser", "Invalid Student Id "+stdId);
			//session.setAttribute("notavilable", "Book is not available");
			
			return "redirect:/booksissue/issuebookform";
	    	
	    }
	
	    
	    else
	    {
	    	
	    	User user=userservice.getUserById(stdId);
	    	
	    	String firstName=user.getFirstName();
	    	String lastName=user.getLastName();
	    	
	    	bookissue.setFirstName(firstName);
	    	bookissue.setLastName(lastName);
	    	
	    	
	    }
	
		Integer count=service.noofBooksIssued(bookissue.getStdId());
		System.out.println("No of books borrowed"+count);
		
		if(count>=3)
		{
			session.setAttribute("count", "BookLimit is done");
			
			return "redirect:/booksissue/issuebookform ";
		}
		
		
		
		Integer bookid=bookissue.getBookId();
		Boolean exist=bookservice.isBookExist(bookid);
		
		
		
		
		System.out.println(exist);
         if(exist==false)
         {
        	 
        	 System.out.println("book is null");
        	 session.setAttribute("nobook", "Invalid Book Id "+bookid);
 			//session.setAttribute("notavilable", "Book is not available");
 			
 			return "redirect:/booksissue/issuebookform";
         }
         else
		{
			 Book book=bookservice.getBookbyId(bookid);
			 String authorName=book.getAuthorName();
			 String bookName=book.getTitle();
				bookissue.setBookName(bookName);
				bookissue.setAuthorName(authorName);
				System.out.println(bookName);
				System.out.println(authorName);
			 	if(bookissue.getBookId()==book.getBookId())
				{
					
					noofcopies=book.getNoofCopies();
					System.out.println("No of copies available are "+noofcopies);
					if(noofcopies==0)
					{
						session.setAttribute("nomorecopies", "No Availability");
						return "redirect:/booksissue/issuebookform";
						
						
					}
					
					else
					{
					book.setNoofCopies(noofcopies-1);
					bookservice.updateBooks(book);	
					
					}
				}
		}
		
	
		
		SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
		String formattedDate = df.format(new Date());
		System.out.println(formattedDate);
		
		String issueDate=bookissue.getIssueDate();
		String dueDate;
		
		
		ServiceFunctions sv=new ServiceFunctions();
		
	dueDate=sv.calculateDuedate(issueDate);				
		long fine=sv.calculateFine(dueDate);
		

		String status=sv.bookStatus();
		bookissue.setDueDate(dueDate);
		bookissue.setFine(fine);
		bookissue.setBookStatus(status);
		
		service.saveBookIssue(bookissue);
		
		logger.info("Book is Issued for the student with id "+bookissue.getStdId());
		
		session.setAttribute("issuebook", "Book with Id "+bookissue.getBookId()+" is issued to student with id  "+bookissue.getStdId());
		
		return "redirect:/booksissue/issuedbookslist";
	}
	
	@GetMapping("/issuedbookslist")
	public String  booksIssued(Map<String,Object> model)
	{
		
		List<BookIssue> booklist=service.getIssuedBooks();
		
		System.out.println(booklist);
		
		model.put("booklist", booklist);
		
		return "list-issuedbooks";
	}
	
    @GetMapping("/deleteissuebook")
	public String deleteBookIssued(@RequestParam Integer transId,HttpSession session)
	{
    	service.deleteIssuedBook(transId);
    	
    	logger.info("Record has been deleted for transaction id : "+transId);
    	 session.setAttribute("deleteissue", "Book Issued with transaction id "+ transId+" is deleted ");
		
		return "redirect:/booksissue/issuedbookslist";
	}
    
   
    @GetMapping("/showFormForUpdate")
    public String updateBookIssuedForm(@RequestParam Integer transId ,Map<String,Object> model)
    {
    	
    	BookIssue bookissue=service.getIssuedBookByTansId(transId);
    			
    	model.put("bookissue", bookissue);
       
    	
    	return "bookissueupdateform";
    	
    }
    
	@PostMapping("/saveupdate")
	public String saveUpdateOfIssuedBook(@ModelAttribute BookIssue bookissue,HttpSession session)	{
		    service.updateIssuedBook(bookissue);
		    session.setAttribute("updateissue", "Book Issued for Student id "+ bookissue.getStdId()+" is updated ");
		return "redirect:/booksissue/issuedbookslist";
		
	}
	
	
}
