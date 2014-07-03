class SwarmBehaviorsController < ApplicationController
  before_action :set_swarm_behavior, only: [:edit, :update, :destroy]
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
	
  # GET /swarm_behaviors/new
  def new
    @swarm_behavior = SwarmBehavior.new
  end

  # GET /swarm_behaviors/1/edit
  def edit
  end

  # POST /swarm_behaviors
  # POST /swarm_behaviors.json
  def create
    @swarm_behavior = SwarmBehavior.new(swarm_behavior_params)

    respond_to do |format|
      if @swarm_behavior.save
        format.html { redirect_to @swarm_behavior, notice: 'Swarm behavior was successfully created.' }
        format.json { render action: 'show', status: :created, location: @swarm_behavior }
      else
        format.html { render action: 'new' }
        format.json { render json: @swarm_behavior.errors, status: :unprocessable_entity }
      end
    end
  end

  # PATCH/PUT /swarm_behaviors/1
  # PATCH/PUT /swarm_behaviors/1.json
  def update
    respond_to do |format|
      if @swarm_behavior.update(swarm_behavior_params)
        format.html { redirect_to @swarm_behavior, notice: 'Swarm behavior was successfully updated.' }
        format.json { head :no_content }
      else
        format.html { render action: 'edit' }
        format.json { render json: @swarm_behavior.errors, status: :unprocessable_entity }
      end
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
    # Use callbacks to share common setup or constraints between actions.
    def set_swarm_behavior
      @swarm_behavior = SwarmBehavior.find(params[:id])
    end

		def set_next_swarm_behavior
			@swarm_behavior = SwarmBehavior.find_by(rating: 0)
		end
	
    # Never trust parameters from the scary internet, only allow the white list through.
    def swarm_behavior_params
      params.require(:swarm_behavior).permit(:comparator_id, :property_a_id, :property_b_id, :random_property_b, :depth_level, :if_property_ids, :if_action_ids, :else_property_ids, :else_action_ids, :if_number_bank, :else_number_bank, :subbehavior_ids)
    end
end
