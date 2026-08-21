import { Link } from 'react-router-dom';
import { ArrowRight, CalendarCheck, Clock, Shield, Sparkles } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import Navbar from '../components/layout/Navbar';

const FEATURED_SERVICES = [
  {
    name: 'Executive Boardroom',
    type: 'Meeting Room',
    duration: '60 min',
    price: 50,
    image: 'https://images.unsplash.com/photo-1497366216548-37526070297c?auto=format&fit=crop&w=600&q=80',
    description: 'Fully equipped boardroom for up to 12 people with AV setup.',
  },
  {
    name: 'Private Studio',
    type: 'Workspace',
    duration: '60 min',
    price: 15,
    image: 'https://images.unsplash.com/photo-1527192491265-7e15c55b1ed2?auto=format&fit=crop&w=600&q=80',
    description: 'Quiet, focused workspace with ergonomic seating and natural light.',
  },
  {
    name: 'Massage Therapy',
    type: 'Wellness',
    duration: '45 min',
    price: 80,
    image: 'https://images.unsplash.com/photo-1544161515-4ab6ce6db874?auto=format&fit=crop&w=600&q=80',
    description: 'Professional massage therapy session in a calming environment.',
  },
  {
    name: 'Photography Studio',
    type: 'Creative',
    duration: '120 min',
    price: 120,
    image: 'https://images.unsplash.com/photo-1554048612-b6a482bc67e5?auto=format&fit=crop&w=600&q=80',
    description: 'Fully lit photography studio with backdrops and professional gear.',
  },
];

const STEPS = [
  {
    icon: Sparkles,
    title: 'Browse Services',
    description: 'Explore our curated catalog of bookable services and spaces.',
  },
  {
    icon: CalendarCheck,
    title: 'Pick a Time',
    description: 'Choose an available slot that fits your schedule perfectly.',
  },
  {
    icon: Shield,
    title: 'Book Instantly',
    description: 'Secure your spot with a confirmed reservation in seconds.',
  },
];

export default function LandingPage() {
  return (
    <div className="min-h-screen bg-background flex flex-col">
      <Navbar />

      {/* ── Hero ──────────────────────────────────────────────── */}
      <section className="relative overflow-hidden">
        {/* Subtle background decoration */}
        <div className="absolute inset-0 -z-10">
          <div className="absolute top-[-20%] right-[-10%] w-[600px] h-[600px] rounded-full bg-primary/5 blur-3xl" />
          <div className="absolute bottom-[-30%] left-[-10%] w-[500px] h-[500px] rounded-full bg-primary/3 blur-3xl" />
        </div>

        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-20 pb-24 sm:pt-28 sm:pb-32">
          <div className="max-w-3xl">
            <Badge variant="secondary" className="mb-6 px-4 py-1.5 text-sm font-medium shadow-low">
              <Clock className="mr-1.5 h-3.5 w-3.5" />
              Book in under 30 seconds
            </Badge>

            <h1 className="text-4xl sm:text-5xl lg:text-6xl font-bold tracking-tight text-foreground leading-[1.1] mb-6">
              Reserve the perfect
              <span className="text-primary"> space & service</span>
            </h1>

            <p className="text-lg sm:text-xl text-muted-foreground max-w-2xl mb-10 leading-relaxed">
              BookaBeeka makes it effortless to discover, schedule, and manage bookings
              for workspaces, wellness sessions, creative studios, and more.
            </p>

            <div className="flex flex-col sm:flex-row items-start gap-4">
              <Button size="lg" className="h-13 px-8 text-base shadow-raised hover:shadow-overlay transition-shadow" asChild>
                <Link to="/register">
                  Get Started Free
                  <ArrowRight className="ml-2 h-5 w-5" />
                </Link>
              </Button>
              <Button size="lg" variant="outline" className="h-13 px-8 text-base" asChild>
                <Link to="/login/customer">
                  Sign In
                </Link>
              </Button>
            </div>
          </div>
        </div>
      </section>

      {/* ── Featured Services ─────────────────────────────────── */}
      <section className="bg-muted/40 border-y border-border">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20 sm:py-24">
          <div className="text-center mb-14">
            <h2 className="text-3xl sm:text-4xl font-bold tracking-tight text-foreground mb-4">
              Services you can book
            </h2>
            <p className="text-muted-foreground text-lg max-w-2xl mx-auto">
              From professional workspaces to wellness treatments — find what you need and reserve it instantly.
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {FEATURED_SERVICES.map((service) => (
              <Card
                key={service.name}
                className="group overflow-hidden border-border bg-card hover:shadow-raised transition-shadow duration-300"
              >
                <div className="aspect-[4/3] w-full overflow-hidden relative">
                  <img
                    src={service.image}
                    alt={service.name}
                    className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
                  />
                  <Badge className="absolute top-3 left-3 bg-background/90 text-foreground hover:bg-background shadow-low text-xs">
                    {service.type}
                  </Badge>
                </div>
                <CardContent className="p-5">
                  <h3 className="font-semibold text-lg mb-1 text-foreground">{service.name}</h3>
                  <p className="text-sm text-muted-foreground mb-4 line-clamp-2">{service.description}</p>
                  <div className="flex items-center justify-between">
                    <div>
                      <span className="text-xl font-bold text-primary">${service.price}</span>
                      <span className="text-sm text-muted-foreground ml-1">/ {service.duration}</span>
                    </div>
                    <Button size="sm" variant="ghost" className="text-primary hover:text-primary" asChild>
                      <Link to="/login/customer">
                        Book <ArrowRight className="ml-1 h-4 w-4" />
                      </Link>
                    </Button>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>
      </section>

      {/* ── How It Works ──────────────────────────────────────── */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20 sm:py-24">
        <div className="text-center mb-14">
          <h2 className="text-3xl sm:text-4xl font-bold tracking-tight text-foreground mb-4">
            How it works
          </h2>
          <p className="text-muted-foreground text-lg max-w-2xl mx-auto">
            Three simple steps to your next booking.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8 lg:gap-12">
          {STEPS.map((step, i) => (
            <div key={step.title} className="text-center group">
              <div className="mx-auto mb-6 flex h-16 w-16 items-center justify-center rounded-2xl bg-primary/10 text-primary transition-colors group-hover:bg-primary group-hover:text-white">
                <step.icon className="h-7 w-7" />
              </div>
              <div className="text-sm font-semibold text-primary mb-2">Step {i + 1}</div>
              <h3 className="text-xl font-semibold text-foreground mb-2">{step.title}</h3>
              <p className="text-muted-foreground leading-relaxed">{step.description}</p>
            </div>
          ))}
        </div>
      </section>

      {/* ── CTA Banner ────────────────────────────────────────── */}
      <section className="border-t border-border bg-foreground text-background">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16 sm:py-20 flex flex-col sm:flex-row items-center justify-between gap-8">
          <div>
            <h2 className="text-2xl sm:text-3xl font-bold mb-2">Ready to get started?</h2>
            <p className="text-background/70 text-lg">Create your account and make your first booking today.</p>
          </div>
          <div className="flex gap-4 shrink-0">
            <Button size="lg" className="h-12 px-8 shadow-raised" asChild>
              <Link to="/register">Sign Up Free</Link>
            </Button>
            <Button size="lg" variant="outline" className="h-12 px-8 border-background/20 text-background hover:bg-background/10 hover:text-background" asChild>
              <Link to="/login/staff">Staff Portal</Link>
            </Button>
          </div>
        </div>
      </section>

      {/* ── Footer ────────────────────────────────────────────── */}
      <footer className="border-t border-border bg-muted/30">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-primary">
              <span className="font-bold text-white text-sm leading-none">B</span>
            </div>
            <span className="font-semibold text-foreground">BookaBeeka</span>
          </div>
          <p className="text-sm text-muted-foreground">
            © {new Date().getFullYear()} BookaBeeka. All rights reserved.
          </p>
        </div>
      </footer>
    </div>
  );
}
