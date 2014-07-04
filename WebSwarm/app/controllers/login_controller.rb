class LoginController < ApplicationController
	def login_attempt
    authorized_user = User.authenticate(params[:email], params[:password])
    if authorized_user
			session[:user_id] = authorized_user.id
      redirect_to :controller => "welcome", :action => "index"
    else
      redirect_to :back, :flash => { :notice => "Sorry, that email or password is invalid. Try again." }
    end
  end
	def logout
		reset_session
		redirect_to url_for(controller: "welcome", action: "index"), :flash => { :notice => "Thanks for playing! Have a nice day!" }
	end
end
