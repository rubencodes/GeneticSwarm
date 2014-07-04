class SwarmBehavior < ActiveRecord::Base
	before_create :generate_new
	
	def generate_new
		self.comparator_id = rand(3)	#0,1,2
		self.property_a_id = rand(9)	#0,1,2...8
		self.property_b_id = rand(9)	#0,1,2...8
		self.random_property_b 	= rand(0).zero? #0:true, 1:false
		self.depth_level	 			= self.depth_level || 0 #0 unless set
		
		if_length		= rand(9)
		else_length	= rand(9)
		if_property_id_array		= if_length.times.map{ rand(9) }	#array size 0-8 of numbers 0-8
		else_property_id_array	= else_length.times.map{ rand(9) }	#array size 0-8 of numbers 0-8
		self.if_property_ids		= if_property_id_array.join(",")
		self.else_property_ids	= else_property_id_array.join(",")
		self.if_action_ids			= if_length.times.map 	{ rand(3) }.join(",")
		self.else_action_ids		=	else_length.times.map { rand(3) }.join(",")
		if_number_bank_array 		= []
		if_length.times do |i|
			if_number_bank_array.push(random_in_range(if_property_id_array[i]))
		end
		self.if_number_bank			= if_number_bank_array.join(",")
		
		else_number_bank_array 	= []
		else_length.times do |i|
			else_number_bank_array.push(random_in_range(else_property_id_array[i]))
		end
		self.else_number_bank		= else_number_bank_array.join(",")
		
		#behavior properties
		self.velocity_scale						= random_in_range 0
		self.max_speed								= random_in_range 1
		self.normal_speed							= random_in_range 2
		self.neighborhood_radius			= random_in_range 3
		self.separation_weight				= random_in_range 4
		self.alignment_weight					= random_in_range 5
		self.cohesion_weight					= random_in_range 6
		self.pacekeeping_weight				= random_in_range 7
		self.rand_motion_probability	= random_in_range 8
		self.rating										= 0
		
		if rand(2).zero?
			@sub = SwarmBehavior.create(depth_level: self.depth_level+1)
			self.subbehavior_ids = @sub.id
		end
	end
	
	def random_in_range(property_id)
		case property_id
			when 0
				return rand / 10.0
			when 1
				return rand(9) + 2
			when 2
				return rand(9) + 2
			when 3
				return rand(91) + 10
			when 4
				return rand(101)
			when 5
				return rand
			when 6
				return rand
			when 7
				return rand
			when 8
				return rand / 2
		end
	end
	
	def self.get_next_swarm_behavior(user_id)
		@swarm_behaviors 	= User.find_by(id: user_id).swarm_behaviors
		@swarm_behaviors.where(rating: 0).order(:created_at).first
	end
	
	def breed_next_generation(user_id)
			@swarm_behaviors 	= User.find_by(id: user_id).swarm_behaviors
			@TheChosenOnes 		= selection(@swarm_behaviors)
			@NextGeneration		= crossover(@TheChosenOnes)
			@NewPopulation		= mutation (@NextGeneration)
			
	end
	
	#tournament selection
	def selection(behaviors)
		@chosen_for_crossover = []
		until @chosen_for_crossover.length == behaviors.length / 2
			@behavior_one	= behaviors[rand(behaviors.length)]
			@behavior_two	= behaviors[rand(behaviors.length)]

			if @behavior_one.rating > @behavior_two.rating
				@chosen_for_crossover.push @behavior_one
			else
				@chosen_for_crossover.push @behavior_two
			end
		end
		return @chosen_for_crossover
	end
	
	#single-point crossover
	def crossover(behaviors)
		#for every two behaviors
		for(i = 0; i < behaviors.length; i+=2)
			#select two behaviors
			behavior_a = behaviors[i]
			behavior_b = behaviors[i+1]
			#find a point within their chromosome to crossover
			point_a = rand(behavior_a.get_chromosome_length-1)+1
			point_b = rand(behavior_b.get_chromosome_length-1)+1
			
			
			things_to_crossover_a = []
			things_to_crossover_b = []
			
			if_array_a 		= behavior_a.if_property_ids.split(",").zip(behavior_a.if_action_ids.split(","), behavior_a.if_number_bank.split(","))
			else_array_a	= behavior_a.else_property_ids.split(",").zip(behavior_a.else_action_ids.split(","), behavior_a.else_number_bank.split(","))
			
			if_array_b 		= behavior_b.if_property_ids.split(",").zip(behavior_b.if_action_ids.split(","), behavior_b.if_number_bank.split(","))
			else_array_b	= behavior_b.else_property_ids.split(",").zip(behavior_b.else_action_ids.split(","), behavior_b.else_number_bank.split(","))
			
			#traverse chromosome looking for point
			current_point = 0;
			if point_a < current_point += if_array_a.length
				things_to_crossover_a = if_array_a.slice!(point_a)
			elsif point_a < current_point += else_array_a.length
				things_to_crossover_a = else_array_a.slice!(point_a)
			end
			
			#traverse chromosome looking for point
			current_point = 0;
			if point_b < current_point += if_array_b.length
				things_to_crossover_b = if_array_b.slice!(point_b)
			elsif point_b < current_point += else_array_b.length
				things_to_crossover_b = else_array_b.slice!(point_b)
			end
			
			#traverse chromosome looking for point
			current_point = 0;
			if point_a < current_point += if_array_a.length
				if_array_a.concat(things_to_crossover_b)
			elsif point_a < current_point += else_array_a.length
				else_array_a.concat(things_to_crossover_a)
			end
			
			#traverse chromosome looking for point
			current_point = 0;
			if point_b < current_point += if_array_b.length
				if_array_b.concat(things_to_crossover_a)
			elsif point_b < current_point += else_array_b.length
				else_array_b.concat(things_to_crossover_a)
			end
			
			new_a_if_properties, new_a_if_actions, new_a_if_numbers = if_array_a.transpose
			new_a_else_properties, new_a_actions, new_a_else_numbers = else_array_a.transpose
			
			new_b_if_properties, new_b_if_actions, new_b_if_numbers = if_array_b.transpose
			new_b_else_properties, new_b_else_actions, new_b_else_numbers = else_array_b.transpose
			
			
			
			
			
			SwarmBehavior.create(	if_property_ids: 	new_a_if_properties.join(","),
														if_action_ids: 		new_a_if_actions.join(",")
														if_number_bank:		new_a_if_numbers.join(",") )
			
			SwarmBehavior.create(	if_property_ids: 	new_b_if_properties.join(","),
														if_action_ids: 		new_b_if_actions.join(",")
														if_number_bank:		new_b_if_numbers.join(",") )
		end
		
		
	end
	
	def mutation(behaviors)
		
	end
	
	def get_chromosome_length(behavior)
		chromosome_length  = 0;
		chromosome_length += behavior.if_property_ids.split(",").length
		chromosome_length += behavior.else_property_ids.split(",").length
		chromosome_length += behavior.sub_behaviors.length
		behavior.sub_behaviors.each do |b_id|
			b = SwarmBehavior.find(b_id)
			chromosome_length += b.get_chromosome_length
		end
		return chromosome_length
	end
end
