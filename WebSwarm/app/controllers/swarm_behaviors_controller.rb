class SwarmBehaviorsController < ApplicationController
  before_action :set_swarm_behavior, only: [:update, :destroy]
	before_action :set_next_swarm_behavior, only: [:show]

  # GET /swarm_behaviors
  # GET /swarm_behaviors.json
  def index
    @swarm_behaviors = SwarmBehavior.all
  end

  # GET /swarm_behaviors/1
  # GET /swarm_behaviors/1.json
  def show
  end

  # PATCH/PUT /swarm_behaviors/1
  # PATCH/PUT /swarm_behaviors/1.json
  def update
		if @swarm_behavior.update(swarm_behavior_params)
			if SwarmBehavior.get_next_swarm_behavior(session[:user_id])
				redirect_to :back, :flash => { :notice => "Thank you for rating your swarm!" }
			else
				if SwarmBehavior.breed_next_generation(session[:user_id])
					redirect_to :back, :flash => { :notice => "Evolved next generation!" }
				else
					redirect_to :back, :flash => { :notice => "Error during evolution!" }
				end
			end
		else
			redirect_to :back, :flash => { :notice => "Swarm could not be rated." }
		end
  end

  # DELETE /swarm_behaviors/1
  # DELETE /swarm_behaviors/1.json
  def destroy
    @swarm_behavior.destroy
    respond_to do |format|
      format.html { redirect_to swarm_behaviors_url }
      format.json { head :no_content }
    end
  end

  private
		def set_next_swarm_behavior
			if(session[:user_id])
				@swarm_behaviors = SwarmBehavior.get_next_swarm_behavior(session[:user_id]).find_all_subbehaviors
			else
				authenticate_or_request_with_http_basic('WebSwarm') do |email, password|
					if @user = User.find_by(username: email, password: password)
						session[:user_id] = @user.id
						@swarm_behaviors = SwarmBehavior.find(149).find_all_subbehaviors
					end
				end
			end
		end
	
    # Use callbacks to share common setup or constraints between actions.
    def set_swarm_behavior
      @swarm_behavior = SwarmBehavior.find(params[:id])
    end
	
    # Never trust parameters from the scary internet, only allow the white list through.
    def swarm_behavior_params
      params.require(:swarm_behavior).permit(	:rating )
    end
end
