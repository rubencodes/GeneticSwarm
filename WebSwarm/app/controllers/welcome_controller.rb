class WelcomeController < ApplicationController
  def index
		if session[:user_id]
			@user = User.find_by(id: session[:user_id])
			@swarm = SwarmBehavior.get_next_swarm_behavior(session[:user_id]) if @user
			@swarm_behaviors = SwarmBehavior.get_next_swarm_behavior(session[:user_id]).find_all_subbehaviors if @user
		end
  end
end
