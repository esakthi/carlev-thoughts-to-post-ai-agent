import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ThoughtInputComponent } from './thought-input.component';
import { ThoughtsService } from '../../services/thoughts.service';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormsModule } from '@angular/forms';
import { of } from 'rxjs';

describe('ThoughtInputComponent', () => {
  let component: ThoughtInputComponent;
  let fixture: ComponentFixture<ThoughtInputComponent>;
  let thoughtsService: ThoughtsService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ThoughtInputComponent,
        HttpClientTestingModule,
        FormsModule
      ],
      providers: [
        {
          provide: ThoughtsService,
          useValue: {
            getLinkedInStatus: () => of({ authorized: true }),
            getCategories: () => of(['Tech', 'Politics', 'Social', 'Others']),
            getLinkedInAuthUrl: () => of({ authorizationUrl: 'http://example.com', state: 'abc' })
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ThoughtInputComponent);
    component = fixture.componentInstance;
    thoughtsService = TestBed.inject(ThoughtsService);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load categories on init', () => {
    expect(component.categories()).toEqual(['Tech', 'Politics', 'Social', 'Others']);
  });

  it('should emit submitThought when onSubmit is called', () => {
    spyOn(component.submitThought, 'emit');
    component.thought = 'Test thought';
    component.selectedCategory = 'Tech';

    component.onSubmit();

    expect(component.submitThought.emit).toHaveBeenCalledWith({
      thought: 'Test thought',
      platforms: ['LINKEDIN'],
      category: 'Tech',
      additionalInstructions: undefined
    });
  });
});
