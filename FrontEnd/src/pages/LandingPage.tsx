import { Link } from 'react-router-dom';
import { ArrowRight, CalendarCheck, BedDouble, Shield, Hotel } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import Navbar from '../components/layout/Navbar';

const FEATURED_ROOMS = [
  {
    name: 'Deluxe King Room',
    type: 'King Room',
    duration: 'night',
    price: 180,
    image: 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304?auto=format&fit=crop&w=600&q=80',
    description: 'Spacious king room with city views, king-size bed, and premium amenities.',
  },
  {
    name: 'Superior Twin Room',
    type: 'Twin Room',
    duration: 'night',
    price: 140,
    image: 'https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=600&q=80',
    description: 'Comfortable twin room ideal for colleagues or friends, with all essentials.',
  },
  {
    name: 'Executive Suite',
    type: 'Suite',
    duration: 'night',
    price: 320,
    image: 'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=600&q=80',
    description: 'Luxury suite with separate living area, panoramic views, and butler service.',
  },
  {
    name: 'Family Room',
    type: 'Family Room',
    duration: 'night',
    price: 220,
    image: 'https://images.unsplash.com/photo-1586023492125-27b2c045efd7?auto=format&fit=crop&w=600&q=80',
    description: 'Generous family room sleeping up to four, with bunk beds and play area.',
  },
];

const STEPS = [
  {
    icon: Hotel,
    title: 'Browse Rooms',
    description: 'Explore our curated selection of hotel rooms and accommodations.',
  },
  {
    icon: CalendarCheck,
    title: 'Pick Your Dates',
    description: 'Choose your check-in and check-out dates that fit your schedule.',
  },
  {
    icon: Shield,
    title: 'Book Instantly',
    description: 'Secure your room with a confirmed reservation in seconds.',
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
              <BedDouble className="mr-1.5 h-3.5 w-3.5" />
              Book your perfect stay
            </Badge>

            <h1 className="text-4xl sm:text-5xl lg:text-6xl font-bold tracking-tight text-foreground leading-[1.1] mb-6">
              Find your ideal
              <span className="text-primary"> hotel room</span>
            </h1>

            <p className="text-lg sm:text-xl text-muted-foreground max-w-2xl mb-10 leading-relaxed">
              BookaBeeka makes it effortless to discover, book, and manage your hotel reservations.
              From budget rooms to luxury suites — find the perfect accommodation for your stay.
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

      {/* ── Featured Rooms ────────────────────────────────────── */}
      <section className="bg-muted/40 border-y border-border">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20 sm:py-24">
          <div className="text-center mb-14">
            <h2 className="text-3xl sm:text-4xl font-bold tracking-tight text-foreground mb-4">
              Featured accommodations
            </h2>
            <p className="text-muted-foreground text-lg max-w-2xl mx-auto">
              From cozy standard rooms to spacious suites — discover the perfect place for your stay.
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {FEATURED_ROOMS.map((room) => (
              <Card
                key={room.name}
                className="group overflow-hidden border-border bg-card hover:shadow-raised transition-shadow duration-300"
              >
                <div className="aspect-[4/3] w-full overflow-hidden relative">
                  <img
                    src={room.image}
                    alt={room.name}
                    className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
                  />
                  <Badge className="absolute top-3 left-3 bg-background/90 text-foreground hover:bg-background shadow-low text-xs">
                    {room.type}
                  </Badge>
                </div>
                <CardContent className="p-5">
                  <h3 className="font-semibold text-lg mb-1 text-foreground">{room.name}</h3>
                  <p className="text-sm text-muted-foreground mb-4 line-clamp-2">{room.description}</p>
                  <div className="flex items-center justify-between">
                    <div>
                      <span className="text-xl font-bold text-primary">${room.price}</span>
                      <span className="text-sm text-muted-foreground ml-1">/ {room.duration}</span>
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
            Three simple steps to your next hotel booking.
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
            <h2 className="text-2xl sm:text-3xl font-bold mb-2">Ready to book your stay?</h2>
            <p className="text-background/70 text-lg">Create your account and reserve your room today.</p>
          </div>
          <div className="flex gap-4 shrink-0">
            <Button size="lg" className="h-12 px-8 shadow-raised" asChild>
              <Link to="/register">Sign Up Free</Link>
            </Button>
            <Button size="lg" variant="outline" className="h-12 px-8 border-background/20 text-background hover:bg-background/10 hover:text-background" asChild>
              <Link to="/login/staff">Hotel Staff Portal</Link>
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
