class SwarmBehavior < ActiveRecord::Base
	before_create :generate_new
	
	def generate_new
		self.rating					 	= 0 #all new behaviors are unrated
		unless self.comparator_id && self.property_a_id && self.property_b_id #if not evolved, randomly generate
			self.comparator_id 	= rand(3)	#0,1,2
			self.property_a_id 	= rand(9)	#0,1,2...8
			self.property_b_id 	= rand(9)	#0,1,2...8
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

			if rand(2).zero?
				@sub = SwarmBehavior.create(depth_level: self.depth_level+1)
				self.subbehavior_ids = @sub.id
			end
		end
	end
	
	#generates randoms within the proper range for each property
	def random_in_range(property_id)
		case property_id
			when 0 #velocity scale
				return rand / 10.0
			when 1 #max speed
				return rand(9) + 2
			when 2 #normal speed
				return rand(9) + 2
			when 3 #neighborhood radius
				return rand(91) + 10
			when 4 #separation weight
				return rand(101)
			when 5 #alignment weight
				return rand
			when 6 #cohesion weight
				return rand
			when 7 #pacekeeping weight
				return rand
			when 8 #random motion probability
				return rand / 2.0
		end
	end
	
	def self.get_next_swarm_behavior(user_id)
		@swarm_behaviors 	= User.find_by(id: user_id).swarm_behaviors
		@swarm_behaviors.where(rating: 0).order(:created_at).first
	end
	
	def breed_next_generation(user_id)
		@user							= User.find_by(id: user_id)
		@swarm_behaviors 	= @user.swarm_behaviors
		@TheChosenOnes 		= selection(@swarm_behaviors)
		@NextGeneration		= crossover(@TheChosenOnes)
		@NewPopulation		= mutation (@NextGeneration)
		@NewPopulation.each do |x| #add each new behavior to user for evaluation
			@user.swarm_behaviors << x
		end
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
	#crosses over at if condition or
	#crosses over between if and else actions or
	#crosses over at property values
	def crossover(behaviors)
		#will hold newly crossed-over behaviors
		@next_generation = []
		
		#for every two behaviors
		behaviors.each_with_index do | behavior_a, behavior_index |
			#select two behaviors
			behavior_b = behaviors[behavior_index+1]
			
			#set new behavior a defaults (copy of behavior a)
			@new_a_comparator = behavior_a.comparator_id
			@new_a_property_a = behavior_a.property_a
			@new_a_property_b = behavior_a.property_b
			if_array_a 		= behavior_a.if_property_ids.split(",").zip(behavior_a.if_action_ids.split(","), behavior_a.if_number_bank.split(","))
			else_array_a	= behavior_a.else_property_ids.split(",").zip(behavior_a.else_action_ids.split(","), behavior_a.else_number_bank.split(","))
			@new_a_if_properties, @new_a_if_actions, @new_a_if_numbers 				= if_array_a.transpose
			@new_a_else_properties, @new_a_else_actions, @new_a_else_numbers 	= else_array_a.transpose
			@prop_array_a = [ behavior_a.velocity_scale, 
												behavior_a.max_speed, 
												behavior_a.normal_speed, 
												behavior_a.neighborhood_radius, 
												behavior_a.separation_weight, 
												behavior_a.alignment_weight, 
												behavior_a.cohesion_weight, 
												behavior_a.pacekeeping_weight, 
												behavior_a.rand_motion_probability ]
			
			#set new behavior b defaults (copy of behavior b)
			@new_b_comparator = behavior_b.comparator_id
			@new_b_property_a	= behavior_b.property_a
			@new_b_property_b	= behavior_b.property_b
			if_array_b 		= behavior_b.if_property_ids.split(",").zip(behavior_b.if_action_ids.split(","), behavior_b.if_number_bank.split(","))
			else_array_b	= behavior_b.else_property_ids.split(",").zip(behavior_b.else_action_ids.split(","), behavior_b.else_number_bank.split(","))
			@new_b_if_properties, @new_b_if_actions, @new_b_if_numbers 				= if_array_b.transpose
			@new_b_else_properties, @new_b_else_actions, @new_b_else_numbers 	= else_array_b.transpose
			@prop_array_b = [ behavior_b.velocity_scale, 
												behavior_b.max_speed, 
												behavior_b.normal_speed, 
												behavior_b.neighborhood_radius, 
												behavior_b.separation_weight, 
												behavior_b.alignment_weight, 
												behavior_b.cohesion_weight, 
												behavior_b.pacekeeping_weight, 
												behavior_b.rand_motion_probability ]
			
			#find crossover section	(if condition, if/else action blocks, or properties)
			case rand 3
				when 0 #crosses over at if condition					
					case rand(3) #find random crossover point
						when 0 #switch comparator_id
							@new_a_comparator = behavior_b.comparator_id
							@new_b_comparator = behavior_a.comparator_id
						when 1 #switch property_a
							@new_a_property_a = behavior_b.property_a
							@new_b_property_a	= behavior_a.property_a
						when 2 #switch property_b
							@new_a_property_b = behavior_b.property_b
							@new_b_property_b	= behavior_a.property_b
					end
				
				when 1 #crosses over between if and else actions
					point_a = rand(behavior_a.if_property_ids.length)
					point_b = rand(behavior_b.if_property_ids.length)

					actions_to_crossover_a = []
					actions_to_crossover_b = []

					if point_a < if_array_a.length #traverse block looking for point
						actions_to_crossover_a = if_array_a.slice!(point_a)
					else
						actions_to_crossover_a = else_array_a.slice!(point_a)
					end

					if point_b < if_array_b.length #traverse block looking for point
						actions_to_crossover_b = if_array_b.slice!(point_b)
					else
						actions_to_crossover_b = else_array_b.slice!(point_b)
					end

					if point_a < if_array_a.length #traverse block looking for point
						if_array_a.concat(actions_to_crossover_b)
					else
						else_array_a.concat(actions_to_crossover_a)
					end

					if point_b < if_array_b.length #traverse block looking for point
						if_array_b.concat(actions_to_crossover_a)
					else
						else_array_b.concat(actions_to_crossover_a)
					end
					
					#update with crossed over blocks
					@new_a_if_properties, @new_a_if_actions, @new_a_if_numbers 				= if_array_a.transpose
					@new_a_else_properties, @new_a_else_actions, @new_a_else_numbers 	= else_array_a.transpose
					@new_b_if_properties, @new_b_if_actions, @new_b_if_numbers 				= if_array_b.transpose
					@new_b_else_properties, @new_b_else_actions, @new_b_else_numbers 	= else_array_b.transpose
				
				when 2 #crosses over at property values
					point_a = rand(@prop_array_a.length)
					point_b = rand(@prop_array_b.length)
				
					a_props_to_crossover = @prop_array_a.slice!(point_a)
					b_props_to_crossover = @prop_array_b.slice!(point_b)
				
					@prop_array_a.concat(b_props_to_crossover)
					@prop_array_b.concat(a_props_to_crossover)
			end
			
			#create next-gen swarm behaviors, push to array
			@next_generation.push	SwarmBehavior.create(	comparator_id:						@new_a_comparator,
																									property_a_id:						@new_a_property_a,
																									property_b_id:						@new_a_property_b,
																									if_property_ids: 					@new_a_if_properties.join(","),
																									if_action_ids: 						@new_a_if_actions.join(","),
																									if_number_bank:						@new_a_if_numbers.join(","),
																									velocity_scale:						@prop_array_a[0],
																									max_speed:								@prop_array_a[1],
																									normal_speed:							@prop_array_a[2],
																									neighborhood_radius:			@prop_array_a[3],
																									separation_weight:				@prop_array_a[4],
																									alignment_weight:					@prop_array_a[5],
																									cohesion_weight:					@prop_array_a[6],
																									pacekeeping_weight:				@prop_array_a[7],
																									rand_motion_probability:	@prop_array_a[8] )
			
			@next_generation.push	SwarmBehavior.create(	comparator_id:						@new_b_comparator,
																									property_a_id:						@new_b_property_a,
																									property_b_id:						@new_b_property_b,
																									if_property_ids: 					@new_b_if_properties.join(","),
																									if_action_ids: 						@new_b_if_actions.join(","),
																									if_number_bank:						@new_b_if_numbers.join(","),
																									velocity_scale:						@prop_array_b[0],
																									max_speed:								@prop_array_b[1],
																									normal_speed:							@prop_array_b[2],
																									neighborhood_radius:			@prop_array_b[3],
																									separation_weight:				@prop_array_b[4],
																									alignment_weight:					@prop_array_b[5],
																									cohesion_weight:					@prop_array_b[6],
																									pacekeeping_weight:				@prop_array_b[7],
																									rand_motion_probability:	@prop_array_b[8] )
		end
	end
	
	#probabilistically mutate each member of the new population
	def mutation(behaviors)
		behaviors.each do |behavior|
			#mutation probability depends on chromosome length
			@mutation_probability = 0.015 / (12 + 6 * behavior.if_property_ids.length)
			
			#mutate if condition
			behavior.comparator_id 	= rand(3)	if @mutation_probability > rand
			behavior.property_a_id 	= rand(9) if @mutation_probability > rand
			behavior.property_b_id 	= rand(9) if @mutation_probability > rand
			
			#mutate properties
			temp_arr = behavior.if_property_ids.split(",").map do | prop |
				prop = rand(9) if @mutation_probability > rand
			end
			behavior.if_property_ids = temp_arr.join(",")
			
			temp_arr = behavior.else_property_ids.split(",").map do | prop |
				prop = rand(9) if @mutation_probability > rand
			end
			behavior.else_property_ids = temp_arr.join(",")
			
			#mutate actions
			temp_arr = behavior.if_action_ids.split(",").map do | act |
				act = rand(3) if @mutation_probability > rand
			end
			behavior.if_action_ids = temp_arr.join(",")
			
			temp_arr = behavior.else_action_ids.split(",").map do | act |
				act = rand(3) if @mutation_probability > rand
			end
			behavior.else_action_ids = temp_arr.join(",")
			
			#mutate number banks
			if_prop_array 	= behavior.if_property_ids.split(",")
			temp_arr = behavior.if_number_bank.split(",").map.with_index do | num, i |
				num = random_in_range(if_prop_array[i]) if @mutation_probability > rand
			end
			behavior.if_number_bank = temp_arr.join(",")
			
			else_prop_array = behavior.else_property_ids.split(",")
			temp_arr = behavior.else_number_bank.split(",").map.with_index do | num, i |
				num = random_in_range(else_prop_array[i]) if @mutation_probability > rand
			end
			behavior.else_number_bank		= else_number_bank_array.join(",")

			#behavior properties
			behavior.velocity_scale						= random_in_range 0 if @mutation_probability > rand
			behavior.max_speed								= random_in_range 1 if @mutation_probability > rand
			behavior.normal_speed							= random_in_range 2 if @mutation_probability > rand
			behavior.neighborhood_radius			= random_in_range 3 if @mutation_probability > rand
			behavior.separation_weight				= random_in_range 4 if @mutation_probability > rand
			behavior.alignment_weight					= random_in_range 5 if @mutation_probability > rand
			behavior.cohesion_weight					= random_in_range 6 if @mutation_probability > rand
			behavior.pacekeeping_weight				= random_in_range 7 if @mutation_probability > rand
			behavior.rand_motion_probability	= random_in_range 8 if @mutation_probability > rand
			
			behavior.save
		end
	end
end
