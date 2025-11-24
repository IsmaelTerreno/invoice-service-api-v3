#!/bin/bash

stripe listen --forward-to localhost:3120/stripe_webhooks
