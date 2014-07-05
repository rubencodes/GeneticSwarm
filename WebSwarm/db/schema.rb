# encoding: UTF-8
# This file is auto-generated from the current state of the database. Instead
# of editing this file, please use the migrations feature of Active Record to
# incrementally modify your database, and then regenerate this schema definition.
#
# Note that this schema.rb definition is the authoritative source for your
# database schema. If you need to create the application database on another
# system, you should be using db:schema:load, not running all the migrations
# from scratch. The latter is a flawed and unsustainable approach (the more migrations
# you'll amass, the slower it'll run and the greater likelihood for issues).
#
# It's strongly recommended that you check this file into your version control system.

ActiveRecord::Schema.define(version: 20140705222409) do

  # These are extensions that must be enabled in order to support this database
  enable_extension "plpgsql"

  create_table "add_rating_to_swarm_behaviors", force: true do |t|
    t.integer  "rating"
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  create_table "api_keys", force: true do |t|
    t.string   "access_token"
    t.datetime "created_at"
    t.datetime "updated_at"
    t.integer  "level"
    t.integer  "owner_id"
  end

  create_table "clinics", force: true do |t|
    t.string   "name"
    t.string   "street"
    t.string   "city"
    t.string   "state"
    t.integer  "zip"
    t.text     "description"
    t.string   "phone"
    t.decimal  "rating"
    t.text     "hours"
    t.string   "admin_id"
    t.string   "password"
    t.string   "queue_id"
    t.string   "approval"
    t.datetime "created_at"
    t.datetime "updated_at"
    t.float    "latitude"
    t.float    "longitude"
    t.string   "location_id"
  end

  create_table "clinics_insurances", id: false, force: true do |t|
    t.integer  "insurance_id"
    t.integer  "clinic_id"
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  create_table "insurances", force: true do |t|
    t.string   "name"
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  create_table "interesteds", force: true do |t|
    t.string   "first_name"
    t.string   "email"
    t.string   "zip"
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  create_table "patients", force: true do |t|
    t.string   "email"
    t.string   "first_name"
    t.string   "last_name"
    t.string   "phone"
    t.string   "queue_id"
    t.string   "password"
    t.datetime "created_at"
    t.datetime "updated_at"
    t.string   "session_id"
  end

  create_table "superusers", force: true do |t|
    t.string   "username"
    t.string   "password"
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  add_index "superusers", ["username"], name: "index_superusers_on_username", unique: true, using: :btree

  create_table "swarm_behaviors", force: true do |t|
    t.integer  "user_id"
    t.integer  "comparator_id"
    t.integer  "property_a_id"
    t.integer  "property_b_id"
    t.boolean  "random_property_b"
    t.integer  "depth_level"
    t.string   "if_property_ids"
    t.string   "if_action_ids"
    t.string   "else_property_ids"
    t.string   "else_action_ids"
    t.string   "if_number_bank"
    t.string   "else_number_bank"
    t.string   "subbehavior_ids"
    t.datetime "created_at"
    t.datetime "updated_at"
    t.decimal  "velocity_scale"
    t.decimal  "max_speed"
    t.decimal  "normal_speed"
    t.decimal  "neighborhood_radius"
    t.decimal  "separation_weight"
    t.decimal  "cohesion_weight"
    t.decimal  "pacekeeping_weight"
    t.decimal  "rand_motion_probability"
    t.decimal  "alignment_weight"
    t.integer  "rating"
    t.string   "name"
  end

  create_table "users", force: true do |t|
    t.string   "username"
    t.string   "password"
    t.string   "school"
    t.datetime "created_at"
    t.datetime "updated_at"
    t.string   "firstname"
    t.string   "lastname"
  end

end
